# FocusSurfaceView
该库主要参考IsseiAoki的SimpleCropView https://github.com/IsseiAoki/SimpleCropView </br> 
实现了在相机的预览界面指定一个区域的大小，形状和位置，只拍摄该指定区域里的图像 </br>

Supported on API Level 10 and above. </br>

![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/circle.png)
![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/circle_pre.png)
![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/ratio_3_4.png)
![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/ratio_3_4_pre.png)</br>
![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/square.png)
![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/square_pre.png)
![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/free.png)
![](https://github.com/CGmaybe10/FocusSurfaceView/blob/master/screenshots/free_pre.png)</br>
##使用方法：</br>
在工程和module里的build.gradle分别添加
```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
dependencies {
	        compile 'com.github.CGmaybe10:FocusSurfaceView:v1.0.0'
	}
```
```xml       
<com.cymaybe.foucsurfaceview.FocusSurfaceView
        android:id="@+id/preview_sv"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@android:color/transparent"
        android:visibility="visible"
        app:focus_crop_height="267dp"
        app:focus_crop_width="200dp"
        app:focus_frame_can_change="true"
        app:focus_frame_color="@android:color/holo_green_dark"
        app:focus_frame_stroke_weight="1dp"
        app:focus_guide_color="@android:color/holo_green_dark"
        app:focus_guide_show_mode="not_show"
        app:focus_guide_stroke_weight="1dp"
        app:focus_handle_color="@android:color/holo_green_dark"
        app:focus_handle_show_mode="not_show"
        app:focus_handle_size="14dp"
        app:focus_min_frame_size="50dp"
        app:focus_mode="circle"
        app:focus_overlay_color="#66000000"
        app:focus_touch_padding="8dp"/>
```
```java
private FocusSurfaceView previewSFV = (FocusSurfaceView) findViewById(R.id.preview_sv);
mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {             
                    mCamera.takePicture(new Camera.ShutterCallback() {
                        @Override
                        public void onShutter() {
                        }
                    }, null, null, new Camera.PictureCallback() {
                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            Bitmap cropBitmap = previewSFV.getPicture(data);                       
                        }
                    });
                }
            }
        });
```
