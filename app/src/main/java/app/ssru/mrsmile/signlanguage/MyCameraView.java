package app.ssru.mrsmile.signlanguage;

import java.util.List;
import org.opencv.android.JavaCameraView;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.AttributeSet;


public class MyCameraView extends JavaCameraView {

	public MyCameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public List<Size> getResolutionList() {
		return mCamera.getParameters().getSupportedPreviewSizes();
	}

	public void setResolution(Size resolution) {
		disconnectCamera();
		mMaxHeight = resolution.height;
		mMaxWidth = resolution.width;
		connectCamera(getWidth(), getHeight());
	}

	public Size getResolution() {
		return mCamera.getParameters().getPreviewSize();
	}

	public boolean isAutoWhiteBalanceLockSupported() {
		//return mParameters.isAutoWhiteBalanceLockSupported();
		return mCamera.getParameters().isAutoWhiteBalanceLockSupported();
	}

	public boolean getAutoWhiteBalanceLock () {
		//return mParameters.getAutoWhiteBalanceLock();
		return mCamera.getParameters().getAutoWhiteBalanceLock();
	}

	public void setAutoWhiteBalanceLock (boolean toggle) {
		Camera.Parameters params = mCamera.getParameters();
		params.setAutoWhiteBalanceLock(toggle);
		mCamera.setParameters(params);
	}

}
