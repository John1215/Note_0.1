package com.android.john.note;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.john.note.R;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;

import me.drakeet.materialdialog.MaterialDialog;

public class EditActivity extends AppCompatActivity implements View.OnTouchListener {
    public static final String TAG = EditActivity.class.getSimpleName();
    private static final int REQUEST_IMAGE_CAMERA = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private static final int REQUEST_HANDWRITING = 3;
    private static final int REQUEST_IMAGE_SCAN=4;

    //状态栏隐藏
    private void setHideStatus() {
        Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    private ImageButton color_chose;
    private List<String> color_icon;
    private ListAdapter color_grid_adapter;
    private GridView gridView;
    private MaterialDialog ColorChoosePanel;
    private RelativeLayout relativeLayout;
    private TextView mDateTextView;
    private TextView mColorTextView;
    private EditText mTitleEditText;
    private EditText mContentEditText;
    private NoteItem mNoteItem;
    private ImageButton mSaveImageButton;
    private ImageButton mPicImageButton;
    private ImageButton mHandwritingImageButton;
    private MaterialDialog mImageMaterialDialog;
    private Button mCameraButton;
    private Button mGalleryButton;
    private int mContentEditTextWidth;
    private CardView mCardview;
    private ImageButton mTeamButton;
    private MaterialDialog TeamPanel;
    private TextView mTeamSend;
    private TextView mTeamCancel;
    private EditText mTeamAccount;
    private ImageButton mOCRButton;
//    private TessBaseAPI baseApi;

    //手指向右滑动时的最小速度
    private static final int XSPEED_MIN = 200;

    //手指向右滑动时的最小距离
    private static final int XDISTANCE_MIN = 150;

    private static final int YDISTANCE_MIN = 80;

    //记录手指按下时的横坐标。
    private float xDown;

    //记录手指移动时的横坐标。
    private float xMove;


    //用于计算手指滑动的速度。
    private VelocityTracker mVelocityTracker;

    public static Intent newIntent(Context context) {
        Intent i = new Intent(context, EditActivity.class);
        return i;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHideStatus();
        setContentView(R.layout.activity_edit);
        initData();
        initView();
        relativeLayout = (RelativeLayout) findViewById(R.id.edit_main);
        relativeLayout.setOnTouchListener(this);
        mCardview=(CardView)findViewById(R.id.edit_cardview);
        mImageMaterialDialog = new MaterialDialog(this);
        View selectView = LayoutInflater.from(this).inflate(R.layout.img_popup_window, null, false);
        mImageMaterialDialog.setCanceledOnTouchOutside(true);
        mImageMaterialDialog.setView(selectView);
        //mImageMaterialDialog.

        mCameraButton = (Button) selectView.findViewById(R.id.camera_button);
        mGalleryButton = (Button) selectView.findViewById(R.id.gallery_button);
        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getImage = new Intent(Intent.ACTION_GET_CONTENT);
                getImage.addCategory(Intent.CATEGORY_OPENABLE);
                getImage.setType("image/*");
                startActivityForResult(getImage, REQUEST_IMAGE_GALLERY);
            }
        });
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] perms = {"android.permission.CAMERA"};
                int permsRequestCode = 200;
                requestPermissions(perms, permsRequestCode);
            }
        });
        mHandwritingImageButton = (ImageButton) findViewById(R.id.edit_handwriting);
        mHandwritingImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(EditActivity.this, HandwritingActivity.class);
                startActivityForResult(i, REQUEST_HANDWRITING);
            }
        });
        mColorTextView = (TextView) findViewById(R.id.edit_color);
        mContentEditText = (EditText) findViewById(R.id.edit_content);
        mContentEditTextWidth = mContentEditText.getWidth();
        mTitleEditText = (EditText) findViewById(R.id.edit_title);
        mDateTextView = (TextView) findViewById(R.id.edit_time);
        mSaveImageButton = (ImageButton) findViewById(R.id.edit_save);
        mSaveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivity();
            }
        });
        mPicImageButton = (ImageButton) findViewById(R.id.edit_pic_add);
        mPicImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImageMaterialDialog.show();
//
            }
        });
        TeamPanel =new MaterialDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.team_popup_window, null);
        TeamPanel.setView(view);
        TeamPanel.setCanceledOnTouchOutside(true);
        mTeamButton=(ImageButton)findViewById(R.id.edit_team);
        mTeamAccount=(EditText)view.findViewById(R.id.team_account);
        mTeamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TeamPanel.show();
            }
        });
        mTeamSend=(TextView)view.findViewById(R.id.team_send);
        mTeamCancel=(TextView)view.findViewById(R.id.team_cancel);
        mTeamSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),mTeamAccount.getText().toString(),Toast.LENGTH_SHORT).show();

            }
        });
        mTeamCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TeamPanel.dismiss();
            }
        });
        mOCRButton=(ImageButton) findViewById(R.id.edit_ocr);
        mOCRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplication(),"yes",Toast.LENGTH_SHORT).show();
                Intent getImage = new Intent(Intent.ACTION_GET_CONTENT);
                getImage.addCategory(Intent.CATEGORY_OPENABLE);
                getImage.setType("image/*");
                startActivityForResult(getImage, REQUEST_IMAGE_SCAN);
            }
        });





        UUID id = (UUID) getIntent().getSerializableExtra("UUID");

        mNoteItem = NoteLab.get(getApplicationContext()).getNoteItem(id);
        mColorTextView.setBackgroundColor(Color.parseColor(mNoteItem.getColor()));
        mTitleEditText.setText(mNoteItem.getTitle());
        String content = mNoteItem.getContent();
        if (content == null) {
            mContentEditText.setText(content);
        } else {
            SpannableString spannableString = new SpannableString(content);
            Pattern p = Pattern.compile("<img>(.+?)</img>");
            Matcher m = p.matcher(content);
            while (m.find()) {
                String[] first = m.group().split("<img>");
                String[] second = first[1].split("</img>");
                String path = second[0];
                Log.i(TAG, "file path after split:" + path);
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                ImageSpan imageSpan = new ImageSpan(this, bitmap);
                spannableString.setSpan(imageSpan, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            mContentEditText.setText(spannableString);
        }
        mDateTextView.setText(mNoteItem.getFormatDateString());


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                baseApi = new TessBaseAPI();
//                //初始化OCR的训练数据路径与语言
//                baseApi.init(TESSBASE_PATH, CHINESE_LANGUAGE);
//            }
//        }).start();
//        baseApi = new TessBaseAPI();
//        //初始化OCR的训练数据路径与语言
//        baseApi.init(TESSBASE_PATH, CHINESE_LANGUAGE);
        //设置识别模式
//        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);

//
    }

    @Override
    protected void onPause() {
        super.onPause();

        mNoteItem.setTitle(mTitleEditText.getText().toString());
        mNoteItem.setContent(mContentEditText.getText().toString());
        mNoteItem.setDate(new Date());
        NoteLab.get(getApplicationContext()).updateNoteItem(mNoteItem);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ContentResolver resolver = getContentResolver();
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAMERA) {
                mImageMaterialDialog.dismiss();
                Uri originUri = data.getData();
                try {
                    Bitmap originBitmap = BitmapFactory.decodeStream(resolver
                            .openInputStream(originUri));
                    float width = mContentEditText.getWidth() * 1.0f;
                    float imgWidth = originBitmap.getHeight() * 1.0f;
                    Matrix matrix = new Matrix();
                    Log.i(TAG, "scale:" + width / imgWidth);
                    matrix.setScale(width / imgWidth, width / imgWidth);
                    matrix.postRotate(90.0f);
                    Bitmap dstMap = Bitmap.createBitmap(originBitmap,
                            0,
                            0,
                            originBitmap.getWidth(),
                            originBitmap.getHeight(),
                            matrix,
                            true);
                    String imagePath = saveImage(getApplicationContext(), dstMap);
                    if (originBitmap == null) {
                        Toast.makeText(this, "cannot get image", Toast.LENGTH_SHORT).show();
                    } else {
                        insertIntoEditText(getBitmapString(getApplicationContext(),
                                dstMap, originUri, imagePath));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }else if(requestCode == REQUEST_IMAGE_SCAN){
                Uri originUri=data.getData();
                Toast.makeText(getApplicationContext(),"正在识别请稍后",Toast.LENGTH_LONG).show();
                mOCRButton.setClickable(false);
                new OCRTask().execute(scaledownBitmap(originUri));
//                String text =OCR(scaledownBitmap(originUri));

//                Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
//                mContentEditText.append(text);
            }
            else if(requestCode == REQUEST_IMAGE_GALLERY){
                mImageMaterialDialog.dismiss();
                Uri originUri = data.getData();
                try {
                    Bitmap originBitmap = BitmapFactory.decodeStream(resolver
                            .openInputStream(originUri));
                    float width = mContentEditText.getWidth() * 1.0f;
                    float imgWidth = originBitmap.getWidth() * 1.0f;
                    Matrix matrix = new Matrix();
                    Log.i(TAG, "scale:" + width / imgWidth);
                    matrix.setScale(width / imgWidth, width / imgWidth);
//                    matrix.postRotate(90.0f);
                    Bitmap dstMap = Bitmap.createBitmap(originBitmap,
                            0,
                            0,
                            originBitmap.getWidth(),
                            originBitmap.getHeight(),
                            matrix,
                            true);
                    String imagePath = saveImage(getApplicationContext(), dstMap);
                    if (originBitmap == null) {
                        Toast.makeText(this, "cannot get image", Toast.LENGTH_SHORT).show();
                    } else {
                        insertIntoEditText(getBitmapString(getApplicationContext(),
                                dstMap, originUri, imagePath));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }else{
                //REQUEST_HANDWRITING
                byte [] bis = data.getByteArrayExtra("bitmap");
                Bitmap originBitmap = BitmapFactory.decodeByteArray(bis, 0, bis.length);
                float width = mContentEditText.getWidth() * 1.0f;
                float imgWidth = originBitmap.getWidth() * 1.0f;
                Matrix matrix = new Matrix();
                Log.i(TAG, "scale:" + width / imgWidth);
                Log.i(TAG, "content width:" + width + ", img width:" + imgWidth);
                matrix.setScale(width / imgWidth, width / imgWidth);
//                    matrix.postRotate(90.0f);
                Bitmap dstMap = Bitmap.createBitmap(originBitmap,
                        0,
                        0,
                        originBitmap.getWidth(),
                        originBitmap.getHeight(),
                        matrix,
                        true);
                String imagePath = saveImage(getApplicationContext(), dstMap);
                if (originBitmap == null) {
                    Toast.makeText(this, "cannot get image", Toast.LENGTH_SHORT).show();
                } else {
                    insertIntoEditText(getBitmapString(getApplicationContext(),
                            dstMap, null, imagePath));
                }
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {

            case 200:

                boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (cameraAccepted) {
                    //授权成功之后，调用系统相机进行拍照操作等
                    Intent getImage = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(getImage, REQUEST_IMAGE_CAMERA);
                } else {
                    //用户授权拒绝之后，友情提示一下就可以了
                }

                break;

        }
    }

    private void initData() {
        color_icon = new ArrayList<>();
        color_icon.add("red");
        color_icon.add("cyan");
        color_icon.add("blue");
        color_icon.add("green");
        color_icon.add("orange");
        color_icon.add("purple");
    }

    private void initView() {
        //颜色选择面板GridView
        color_grid_adapter = new com.android.john.note.ColorButtonAdapter(this, color_icon);
        gridView = new MyGridView(this);
        gridView.setNumColumns(3);
        gridView.setVerticalSpacing(120);
        gridView.setPadding(0, 40, 0, 40);
        gridView.setAdapter(color_grid_adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
               // Toast.makeText(getApplicationContext(), color_grid_adapter.getItem(i).toString(), Toast.LENGTH_SHORT).show();
                changeColor(color_grid_adapter.getItem(i).toString());
                ColorChoosePanel.dismiss();

            }
        });
        //颜色选择面板弹出
        ColorChoosePanel = new MaterialDialog(this);

        ColorChoosePanel.setContentView(gridView);
        ColorChoosePanel.setCanceledOnTouchOutside(true);
        //底部ImageButton
        color_chose = (ImageButton) findViewById(R.id.edit_color_choose);
        color_chose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorChoosePanel.show();
            }
        });
       ListView listView;

    }

    private void changeColor(String color) {
        int id;
        switch (color) {
            case "red":
                id = R.color.red;
                mNoteItem.setColor("#FF5252");
                break;
            case "cyan":
                id = R.color.cyan;
                mNoteItem.setColor("#00BCD4");
                break;
            case "green":
                id = R.color.green;
                mNoteItem.setColor("#4CAF50");
                break;
            case "blue":
                id = R.color.blue;
                mNoteItem.setColor("#03A9F4");
                break;
            case "purple":
                id = R.color.purple;
                mNoteItem.setColor("#673AB7");
                break;
            case "orange":
                id = R.color.orange;
                mNoteItem.setColor("#FF9800");
                break;
            default:
                id = R.color.red;
        }
        mColorTextView.setBackgroundResource(id);
    }

    private void finishActivity() {
        finish();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        createVelocityTracker(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xDown = event.getRawX();

                break;
            case MotionEvent.ACTION_MOVE:
                xMove = event.getRawX();

                //活动的距离
                int distanceX = (int) (xMove - xDown);

                //获取瞬时速度
                int xSpeed = getScrollVelocity();
                //当滑动的距离大于我们设定的最小距离且滑动的瞬间速度大于我们设定的速度时，返回到上一个activity
                if (distanceX > XDISTANCE_MIN && xSpeed > XSPEED_MIN) {
//                    Toast.makeText(this, "返回", Toast.LENGTH_SHORT).show();
                    //finish();
                    finishActivity();
                }

                break;
            case MotionEvent.ACTION_UP:
                recycleVelocityTracker();
                break;
            default:
                break;
        }
        return true;
    }

    private String saveImage(Context context, Bitmap bitmap) {
        File dir = new File(context.getFilesDir(), "Images");
        System.out.println("directory:" + context.getFilesDir());
        if (!dir.exists()) {
            System.out.println("isSuccess?" + dir.mkdirs());
        }
        String filename = "IMG_" + System.currentTimeMillis() + ".jpg";
        File file = new File(dir, filename);
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        String imagePath = context.getFilesDir() + "/Images/" + filename;
        Log.i(TAG, "saved file path:" + imagePath);
        return imagePath;
    }

    private SpannableString getBitmapString(Context context, Bitmap bitmap, Uri uri, String path) {
        SpannableString spannableString = new SpannableString("<img>" + path + "</img>");
        ImageSpan imageSpan = new ImageSpan(this, bitmap);
        spannableString.setSpan(imageSpan, 0, spannableString.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private void insertIntoEditText(SpannableString spannableString) {
        Editable editable = mContentEditText.getText();
        int start = mContentEditText.getSelectionStart();
        editable.insert(start, "\n");
        editable.insert(start + 1, spannableString);
        editable.insert(start + spannableString.length() + 1, "\n");
        mContentEditText.setText(editable);
        mContentEditText.setSelection(start + spannableString.length() + 2);
    }


    //创建VelocityTracker对象，并将触摸content界面的滑动事件加入到VelocityTracker当中。
    private void createVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    // 回收VelocityTracker对象。

    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    /**
     * 获取手指在content界面滑动的速度。
     *
     * @return 滑动速度，以每秒钟移动了多少像素值为单位。
     */
    private int getScrollVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) mVelocityTracker.getXVelocity();
        return Math.abs(velocity);
    }
    public Bitmap scaledownBitmap(Uri uri){

        ContentResolver resolver = getContentResolver();
              long startTime= System.currentTimeMillis();

                int maxSize=1024*1024*1024;
                int ratio=1;

                Bitmap tmp=null;
                Bitmap scaled_bitmap=null;
                try {
                        tmp=BitmapFactory.decodeStream(resolver.openInputStream(uri));
                    if(tmp.getByteCount()>maxSize)
                       ratio =tmp.getByteCount()/maxSize;
                    else
                        ratio=1;

                    BitmapFactory.Options options=new BitmapFactory.Options();
                    options.inSampleSize=ratio;
                    scaled_bitmap=BitmapFactory.decodeStream(resolver.openInputStream(uri),null,options);

                    Log.v("originSize",String.valueOf(tmp.getByteCount()));
                    Log.v("ratio",String.valueOf(ratio));
                    Log.v("scaledSize",String.valueOf(scaled_bitmap.getByteCount()));


                } catch (Exception e) {
                        e.printStackTrace();
                     }
        long endTime=System.currentTimeMillis();
        Log.v("scale",String.valueOf(endTime-startTime));

                 return scaled_bitmap;
            }

    static final String CHINESE_LANGUAGE = "chi_sim";
    static final String TESSBASE_PATH = "/storage/emulated/0/tesseract/";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public String OCR(Bitmap bitmap){
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        Bitmap bmp =  bitmap;
        bmp = bitmap2Gray(bmp);
//        bmp = gray2Binary(bmp);
        long startTime= System.currentTimeMillis();
        TessBaseAPI baseApi = new TessBaseAPI();
        //初始化OCR的训练数据路径与语言
        baseApi.init(TESSBASE_PATH, CHINESE_LANGUAGE);
        //设置识别模式
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        long endTime= System.currentTimeMillis();
        Log.v("init",String.valueOf(endTime-startTime));
        //设置要识别的图片
        baseApi.setImage(bmp);
        String text = baseApi.getUTF8Text();
        baseApi.clear();
        baseApi.end();
//        Toast.makeText(getApplicationContext(),text,Toast.LENGTH_SHORT).show();
        return text;
    }

    public Bitmap bitmap2Gray(Bitmap bmSrc) {
        // 得到图片的长和宽
        long startTime=System.currentTimeMillis();
        int width = bmSrc.getWidth();
        int height = bmSrc.getHeight();
        // 创建目标灰度图像
        Bitmap bmpGray = null;
        bmpGray = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 创建画布
        Canvas c = new Canvas(bmpGray);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmSrc, 0, 0, paint);
        long endTime= System.currentTimeMillis();
        Log.v("gray",String.valueOf(endTime-startTime));
        return bmpGray;
    }

    public class OCRTask extends AsyncTask<Bitmap,Void,String>{
        long startTime= System.currentTimeMillis();
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Log.i(TAG, "OCR ing.......");

            return OCR(bitmaps[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
            mContentEditText.append(s);
            Toast.makeText(getApplicationContext(),"识别完成！",Toast.LENGTH_SHORT).show();
            mOCRButton.setClickable(true);
            long endTime= System.currentTimeMillis();
            Log.v("ocr",String.valueOf(endTime-startTime));
        }



    }

}
