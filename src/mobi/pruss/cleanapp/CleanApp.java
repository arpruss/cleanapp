package mobi.pruss.cleanapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mobi.pruss.cleanapp.R;

public class CleanApp extends Activity {
	ArrayList<String> mounted;
	ArrayList<String> orphans;
	int	orphanSize;
	
	private void fatalError(String title, String msg) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        
        Log.e("CleanApp", title);

        alertDialog.setTitle(title);
        alertDialog.setMessage(msg);
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"OK", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {finish();} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {finish();} });
        alertDialog.show();		
	}
	
	private void deleteOrphans() {		
		String cmd = "rm";
		
		for (String o:orphans) {
			cmd += " " + "/mnt/secure/asec/" + o;
		}
		
		try {
			Process p;		
			p = Runtime.getRuntime().exec("su");
			DataOutputStream suIn  = new DataOutputStream(p.getOutputStream());
			
			suIn.writeChars(cmd + " >/dev/null 2> /dev/null\n");
			Log.v("delete", cmd);
			suIn.close();
			
			p.waitFor();
			Toast.makeText(this, "Delete command done!", Toast.LENGTH_SHORT).show();
			} catch (Exception e) {
			Toast.makeText(this, "Error deleting.", Toast.LENGTH_SHORT).show();				
			}
	}
	
	private String getOrphanNames() {
		String out = "";
		
		for (String o:orphans) {
			out += "•" + o + "\n";
		}
		
		return out;
	}
	
	private void handleOrphans() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        
        alertDialog.setTitle("Orphan app files found");
        alertDialog.setMessage("The following orphan files have been found on /mnt/secure/asec, " +
        		"occupying a total space of " + orphanSize + "kB:\n" + 
        		getOrphanNames() + "\nDo you wish to delete them?" );
        		
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"Yes", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {deleteOrphans(); finish();} });
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, 
        		"No", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {finish();} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {finish();} });
        alertDialog.show();		
	}
	
	private void handleNoOrphans() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        
        alertDialog.setTitle("No orphan app files found");
        alertDialog.setMessage("Hurray!  No orphan app files have been found on /mnt/secure/asec. "+
        		"Come back another day.");
        		
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, 
        		"OK", 
        	new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {finish();} });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {finish();} });
        alertDialog.show();		
	}
	


	private boolean getMounted() {
		mounted = new ArrayList<String>();
		boolean haveAsec = false;
		
		try {
			Process p = Runtime.getRuntime().exec("mount");
		DataInputStream mountStream = new DataInputStream(p.getInputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(mountStream));
		String line;
		Pattern pat = Pattern.compile("/[^ ]+ on /mnt/asec/([^ /]+) type");
		while((line=br.readLine())!=null) {
			line = line.trim();
			if (!haveAsec && line.contains("/mnt/asec"))
				haveAsec = true;
			Matcher m = pat.matcher(line);
			if (m.find()) {
				mounted.add(m.group(1));				
			}
		}
		return haveAsec;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		} 
	}
	
	private boolean isOrphan(String s) {
		for(String m:mounted) {
			if (m.equals(s))
				return false;
		}
		return true;
	}
	
	private boolean getOrphans() {
		orphans = new ArrayList<String>();
		orphanSize = 0;
		
		try {
		Process p;		
		p = Runtime.getRuntime().exec("su");
		DataOutputStream suIn  = new DataOutputStream(p.getOutputStream());
		DataInputStream suOut = new DataInputStream(p.getInputStream());
		
		suIn.writeChars("ls -1s /mnt/secure/asec\n");
		suIn.close();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(suOut));
		String line;
		Pattern pat = Pattern.compile("[^ ]*([0-9]+) +([^ ]+)\\.asec");
		while((line=br.readLine())!=null) {
			Matcher m = pat.matcher(line);
			line = line.trim();
			if (m.find()) {
				if (isOrphan(m.group(2))) {
					orphanSize += Integer.parseInt(m.group(1));
					orphans.add(m.group(2)+".asec");
				}
			}
		}
		return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			return false;
		}
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
    
    public void onStart() {
    	super.onStart();
    	if (!getMounted()) {
    		fatalError("Fatal error", "Cannot fetch mount list.");
    		return;
    	}
    	if (!getOrphans()) {
			fatalError("Fatal error", "Cannot list data in /mnt/secure/asec.  Perhaps your device is not rooted?");
			return;
    	}
    	if (orphans.size() == 0)
    		handleNoOrphans();
    	else
    		handleOrphans();
    }
}
