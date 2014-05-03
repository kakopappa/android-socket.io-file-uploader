package com.socketiofileuploader;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.socketio.Acknowledge;
import com.koushikdutta.async.http.socketio.EventCallback;
import com.koushikdutta.async.http.socketio.SocketIOClient;
import com.koushikdutta.async.http.socketio.SocketIORequest;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	private static final String URL = "http://192.168.0.103:8090";
	private static final String UPLOAD_FILE_PATH = "/sdcard/com.irule.activity-1.apk"; // Make sure file exists ..	
	
	private SocketIOClient mClient;
	private FileUploadManager mFileUploadManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mFileUploadManager = new FileUploadManager();
		new FileUploadTask().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private class FileUploadTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			boolean isSuccess = connect();
			
			if(isSuccess) {	
				mFileUploadManager.prepare(UPLOAD_FILE_PATH, MainActivity.this);
				
				// This function gets callback when server requests more data 
				setUploadFileMoreDataReqListener(mUploadFileMoreDataReqListener);
				
				// This function will get a call back when upload completes
				setUploadFileCompleteListener();
				
				// Tell server we are ready to start uploading ..
				if(mClient.isConnected()) {		
		 			JSONArray jsonArr = new JSONArray();
		 			JSONObject res = new JSONObject(); 
		 			
		 	        try {
		 	        	res.put("Name", mFileUploadManager.getFileName());
		 	        	res.put("Size", mFileUploadManager.getFileSize());		 	        	
						jsonArr.put(res);
						
						// This will trigger server 'uploadFileStart' function
						mClient.emit("uploadFileStart", jsonArr);	
		 			} catch (JSONException e) {
		 				//TODO: Log errors some where..
					}	
		 		}	
			}
			
			return null;
		}

		@Override
		protected void onPostExecute(String result) { }

		@Override
		protected void onPreExecute() { }

		@Override
		protected void onProgressUpdate(Void... values) { }
	}
	
	private UploadFileMoreDataReqListener mUploadFileMoreDataReqListener = new UploadFileMoreDataReqListener() {
		
		@Override
		public void uploadChunck(int offset, int percent) {
			 Log.v(TAG, String.format("Uploading %d completed. offset at: %d", percent,  offset));
			 
			 try {
				 
				// Read the next chunk
				mFileUploadManager.read(offset);
				
				if(mClient.isConnected()) {		
		 			JSONArray jsonArr = new JSONArray();
		 			JSONObject res = new JSONObject(); 
		 			
		 	        try {
		 	        	res.put("Name", mFileUploadManager.getFileName());
		 	        	res.put("Data", mFileUploadManager.getData());
		 	        	res.put("chunkSize", mFileUploadManager.getBytesRead());	
						jsonArr.put(res);
						
						// This will trigger server 'uploadFileChuncks' function
						mClient.emit("uploadFileChuncks", jsonArr);	
		 			} catch (JSONException e) {
		 				//TODO: Log errors some where..
					}	
		 		}	
				
			} catch (IOException e) {
				 
			}
			 
		}
		
		@Override
		public void err(JSONException e) {
			// TODO Auto-generated method stub
			
		}
	};
	
	private void setUploadFileMoreDataReqListener(final UploadFileMoreDataReqListener callback ) {
 		if(mClient != null) {
 			EventCallback eventCallback = new EventCallback() {
 		 		@Override
 				public void onEvent(JSONArray argument, Acknowledge acknowledge) {
 		 			for (int i = 0; i < argument.length(); i++) {
 						 try {
 							JSONObject json_data = argument.getJSONObject(i);
 							int place = json_data.getInt("Place");
 							int percent = json_data.getInt("Percent"); 							
 							
 							callback.uploadChunck(place, percent); 							
 							break;
 						} catch (JSONException e) {
 							callback.err(e);
 						}						 	
 					}
 		 		}
 	 		};
 	 		
 	 		mClient.addListener("uploadFileMoreDataReq", eventCallback); 
 		}	
 	}
	
	private void setUploadFileCompleteListener() {
 		if(mClient != null) {
 			EventCallback eventCallback = new EventCallback() {
 		 		@Override
 				public void onEvent(JSONArray argument, Acknowledge acknowledge) {
 		 			 mFileUploadManager.close();
 		 		}
 	 		};
 	 		
 	 		mClient.addListener("uploadFileCompleteRes", eventCallback); 
 		}	
 	}
	
	
	
	public boolean connect () {
		boolean isSuccess = false;
		
 	    try {
 	    	SocketIORequest req = new SocketIORequest(URL);
 	    	req.setLogging("Socket.IO", Log.VERBOSE);
			mClient = SocketIOClient.connect(AsyncHttpClient.getDefaultInstance(), req, null).get();
			isSuccess = true;
  	    } catch (InterruptedException e) {
  	    	isSuccess = false;
		} catch (ExecutionException e) {
			isSuccess = false;
		}
 	    
 	    return isSuccess;
  	}

}
