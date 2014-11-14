package somethingadhoc;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxNetwork extends OSNetwork{
    String networkInfName; // wlan0
    String expectedOS; 
    String[] requiredPrograms;
    
    public LinuxNetwork(String networkInfName){
        this.networkInfName = networkInfName;
        this.expectedOS = "Linux";
        this.requiredPrograms = new String[]{"ifconfig","iw","iwconfig","nmcli","ping"};
        
        int status = checkRequirement();
        if( status != 0 ){
            System.err.println("Error code: "+status);
            System.exit(-1);
        }
        
    }

    @Override
    public final int checkRequirement() {
        // 1. this is linux?
        String osName = getOS();
        if( ! osName.equals(expectedOS) ){
            System.err.println("Invalid OS - "+osName);
            return -1;
        }
        
        // 2. run as root?
        if(! isPrivileged() )
            return -2;
        
        // 3. have required programs (ifconfig, iw, iwconfig)
        if(! hasPrograms(requiredPrograms) )
            return -3;
        
        // 4. have wifi interface
        if(! hasWiFiInterface(networkInfName) )
            return -4;
        
        return 0;
    }
    
    
    @Override
    public boolean hasPrograms(String[] programsList) {
        for (String program: programsList) {
            String result = execCmd("which "+program)[0];
            result = result.replaceAll("\n", "");
            if(result.equals("")){ // not found
                System.err.println("Program: "+program+" is not found.");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isPrivileged(){
        String currentUser = execCmd("whoami")[0];
        if( currentUser.equals("root\n") ){
            return true;
        }
        
        System.err.println("Current User: "+currentUser);
        return false;
    }
    
    @Override
    public boolean hasWiFiInterface(String networkInfName) {
        // @TODO search by regex method
        String result = execCmd("iwconfig")[0];
        if( result.contains(networkInfName) ){
            return true;
        }
        System.err.println("WiFi interface "+networkInfName+" does not exist.");
        return false;
    }
    
    
    @Override
    public int connectAP(String ssid, String ipAddress, String subnetMask) {
        /*
        iwconfig <inf> essid <ssid>
        ifconfig <inf> <ip> netmask <netmask>
        */
        String command = "iwconfig "+networkInfName+" essid "+ssid;
        int exitCode = Integer.parseInt(execCmd(command)[1]);
        if(exitCode == 0){
            exitCode = setupIP(ipAddress, subnetMask);
            return exitCode;
        }
        return -1;
    }
    
    @Override
    public int setupIP(String ipAddress, String subnetMask){
        String[] commands = {
                        // 1. down network interface
			 "ip link set "+networkInfName+" down",
                        // 2. setup IP address, subnetmask
			 "ifconfig "+networkInfName+" "+ipAddress+" netmask "+subnetMask,
                        // 3. up network interface
			 "ip link set "+networkInfName+" up"
		 };
        return execCmds(commands);
    }
    @Override
    public ArrayList<ScannedAPData> scanAvailableAdhoc(){
        /*
        iw wlan0 scan
        nmcli dev wifi list  iface wlan0
        
        sample output:
        SSID                              BSSID               MODE             FREQ       RATE       SIGNAL   SECURITY   ACTIVE
        'ggwp'                            02:11:87:DA:DD:57   Ad-Hoc           2412 MHz   54 MB/s    0        --         no
        */
        // 1. scan AP
        String availableAP = scanAvailableAP();
        // 2. filter only AP name (SSID)
        ArrayList<ScannedAPData> availableAPs = new ArrayList<>();
        String pattern = ".*Ad-Hoc.*";
        
        Pattern p = Pattern.compile(pattern, Pattern.MULTILINE);
	Matcher m = p.matcher(availableAP);
        
        // 3. loop each line of result
        while (m.find()) {
		ScannedAPData ap = new ScannedAPData();
                String lineMatched = m.group(0);
                // 4. extract a line into ScannedAPData property
                // @TODO: cleanup this mess string manipulation
                ap.ssid=lineMatched.substring(lineMatched.indexOf("'"), lineMatched.lastIndexOf("'"));
                ap.bssid=lineMatched.substring(lineMatched.indexOf(":")-2,lineMatched.lastIndexOf(":")+2);
                ap.mode="Ad-Hoc";
                ap.freq="2412 MHz"; // fix this later
                ap.rate="54 MB/s"; // fix this later
                ap.signal="0"; // fix this ASAP
                ap.security="--"; // fix this later
                ap.active="no"; // fix this later
                availableAPs.add(ap);
                System.out.println("Debug: scanAvailableAdhoc() found "+ap.ssid);
	}
        return availableAPs;
    }
    @Override
    public String scanAvailableAP(){
        String command = "nmcli dev wifi list iface "+networkInfName;
        String result = execCmd(command)[0];
        return result;
    }

    @Override
    public int setupAP(String ssid, String mode, String ipAddress, String subnetMask) {
        String[] commands = {
                        // 1. down network interface
			 "ip link set "+networkInfName+" down",
                        // 2. set inferface to ad-hoc mode
			 "iwconfig "+networkInfName+" mode "+mode,
                        // 3. setup ESSID (Boradcast Name)
			 "iwconfig "+networkInfName+" essid " + ssid,
		 };
        int exitCode = execCmds(commands);
        if(exitCode == 0){
            // 4. setup IP
            return setupIP(ipAddress, subnetMask);
        }
        return -1;
    }
    
    @Override
    public boolean pingTest(String targetIP){
        String command = "ping -I "+networkInfName+" -c 3 "+targetIP;
        // 1. ping target ip
        String[] result = execCmd(command);
        int exitCode = Integer.parseInt(result[1]);
        // 2. ping command success
        if( exitCode == 0 ){
            // 3. check result 
            String expectedResult = "bytes from";
            if(result[0].contains(expectedResult)){
                return true;
            }
        }
        return false;
    }

    @Override
    public int downInterface() {
        String command = "ip link set "+networkInfName+" down";
        int exitCode = Integer.parseInt(execCmd(command)[1]);
        return exitCode;
    }
    

}
