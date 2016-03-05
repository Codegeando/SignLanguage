package app.ssru.mrsmile.signlanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class DisplayAppList extends Activity implements OnItemClickListener{

	private ListView listview = null; 
	 private List<AppInfo> mlistAppInfo = null;
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.browse_app_list);
		
		 listview = (ListView) findViewById(R.id.listviewApp);  
	        mlistAppInfo = new ArrayList<AppInfo>();  
	        queryAppInfo();   
	        BrowseApplicationInfoAdapter browseAppAdapter = new BrowseApplicationInfoAdapter(  
	                this, mlistAppInfo);  
	        listview.setAdapter(browseAppAdapter);  
	        listview.setOnItemClickListener(this);
	}
	

	public void queryAppInfo() {  
        PackageManager pm = this.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);  
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);  
       
        List<ResolveInfo> resolveInfos = pm  
                .queryIntentActivities(mainIntent, PackageManager.MATCH_DEFAULT_ONLY);  
       
        Collections.sort(resolveInfos,new ResolveInfo.DisplayNameComparator(pm));  
        if (mlistAppInfo != null) {  
            mlistAppInfo.clear();  
            for (ResolveInfo reInfo : resolveInfos) {  
                String activityName = reInfo.activityInfo.name;   
                String pkgName = reInfo.activityInfo.packageName; 
                String appLabel = (String) reInfo.loadLabel(pm);  
                Drawable icon = reInfo.loadIcon(pm);  
                
                Intent launchIntent = new Intent();  
                launchIntent.setComponent(new ComponentName(pkgName,  
                        activityName));  
               
                AppInfo appInfo = new AppInfo();  
                appInfo.setAppLabel(appLabel);  
                appInfo.setPkgName(pkgName);  
                appInfo.setAppIcon(icon);  
                appInfo.setIntent(launchIntent);  
                mlistAppInfo.add(appInfo); 
            }  
        }  
    }  
	
	   public void onItemClick(AdapterView<?> arg0, View view, int position,long arg3) {         
		   Intent resultIntent = new Intent();
		   resultIntent.putExtra("Position", position);
		   setResult(Activity.RESULT_OK, resultIntent);
		   finish();	      
	 } 
	   
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.display_app_list, menu);
		return true;
	}

}
