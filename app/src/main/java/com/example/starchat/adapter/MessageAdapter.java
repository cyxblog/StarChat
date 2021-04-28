package com.example.starchat.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.starchat.R;
import com.example.starchat.bean.MessageBean;
import com.example.starchat.util.FileUtil;
import com.example.starchat.util.MsgTypeUtil;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * 消息适配器，包括八种消息
 * 1.发送的文本
 * 2.接收的文本
 * 3.发送的图片
 * 4.接收的图片
 * 5.发送的文件
 * 6.接收的文件
 * 7.发送的语音
 * 8.接收的语音
 */
public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final String TAG = "MessageAdapter";

    //左边消息
    public static final int VIEW_TYPE_MSG_LEFT = 0;
    //右边消息
    public static final int VIEW_TYPE_MSG_RIGHT = 1;
    //左边图片
    public static final int VIEW_TYPE_IMG_LEFT = 2;
    //右边图片
    public static final int VIEW_TYPE_IMG_RIGHT = 3;
    //左边文件
    public static final int VIEW_TYPE_FILE_LEFT = 4;
    //右边文件
    public static final int VIEW_TYPE_FILE_RIGHT = 5;
    //左边语音
    public static final int VIEW_TYPE_AUDIO_LEFT = 6;
    //右边语音
    public static final int VIEW_TYPE_AUDIO_RIGHT = 7;

    private Context mContext;
    private List<MessageBean> mMessageBeans;

    public MessageAdapter(List<MessageBean> messageBeans, Context context) {
        mMessageBeans = messageBeans;
        mContext = context;
    }

    /**
     * 得到每一项信息的类型
     * @param position 消息在列表中的位置
     * @return 返回消息类型
     */
    @Override
    public int getItemViewType(int position) {
        switch (mMessageBeans.get(position).type) {
            case MsgTypeUtil.SELF_MSG:
                return VIEW_TYPE_MSG_RIGHT;
            case MsgTypeUtil.OTHERS_MSG:
                return VIEW_TYPE_MSG_LEFT;
            case MsgTypeUtil.SELF_IMG:
                return VIEW_TYPE_IMG_RIGHT;
            case MsgTypeUtil.OTHERS_IMG:
                return VIEW_TYPE_IMG_LEFT;
            case MsgTypeUtil.SELF_FILE:
                return VIEW_TYPE_FILE_RIGHT;
            case MsgTypeUtil.OTHERS_FILE:
                return VIEW_TYPE_FILE_LEFT;
            case MsgTypeUtil.SELF_AUDIO:
                return VIEW_TYPE_AUDIO_RIGHT;
            case MsgTypeUtil.OTHERS_AUDIO:
                return VIEW_TYPE_AUDIO_LEFT;
            default:
                return 0;
        }
    }

    /**
     * 初始化消息的View
     * @param parent 消息view的父view
     * @param viewType getItemViewType()返回的消息类型
     * @return 返回消息View
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_MSG_LEFT:
                view = LayoutInflater.from(mContext).inflate(R.layout.msg_left_list_item, parent, false);
                return new LeftMsgInnerHolder(view);
            case VIEW_TYPE_MSG_RIGHT:
                view = LayoutInflater.from(mContext).inflate(R.layout.msg_right_list_item, parent, false);
                return new RightMsgInnerHolder(view);
            case VIEW_TYPE_IMG_LEFT:
                view = LayoutInflater.from(mContext).inflate(R.layout.img_left_list_item, parent, false);
                return new LeftImgInnerHolder(view);
            case VIEW_TYPE_IMG_RIGHT:
                view = LayoutInflater.from(mContext).inflate(R.layout.img_right_list_item, parent, false);
                return new RightImgInnerHolder(view);
            case VIEW_TYPE_FILE_LEFT:
                view = LayoutInflater.from(mContext).inflate(R.layout.file_left_list_item, parent, false);
                return new LeftFileInnerHolder(view);
            case VIEW_TYPE_FILE_RIGHT:
                view = LayoutInflater.from(mContext).inflate(R.layout.file_right_list_item, parent, false);
                return new RightFileInnerHolder(view);
            case VIEW_TYPE_AUDIO_LEFT:
                view = LayoutInflater.from(mContext).inflate(R.layout.audio_left_list_item, parent, false);
                return new LeftAudioInnerHolder(view);
            case VIEW_TYPE_AUDIO_RIGHT:
                view = LayoutInflater.from(mContext).inflate(R.layout.audio_right_list_item, parent, false);
                return new RightAudioInnerHolder(view);
            default:
                return null;
        }
    }

    /**
     * 绑定消息对应的组件和事件
     * @param holder 消息内部类
     * @param position 消息位置
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {

        if (holder instanceof LeftMsgInnerHolder) {
            ((LeftMsgInnerHolder) holder).setData(mMessageBeans.get(position), mContext);
            ((LeftMsgInnerHolder) holder).mTextMsg.setCustomSelectionActionModeCallback(new ActionMode.Callback2() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater menuInflater = mode.getMenuInflater();
                    if (menuInflater != null) {
                        menuInflater.inflate(R.menu.popup_menu, menu);
                    }
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                    switch (item.getItemId()) {
                        case R.id.func_delete:
                            mMessageBeans.remove(position);
                            notifyDataSetChanged();
                            break;
                    }

                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {

                }
            });

        } else if (holder instanceof RightMsgInnerHolder) {
            ((RightMsgInnerHolder) holder).setData(mMessageBeans.get(position), mContext);
            ((RightMsgInnerHolder) holder).mTextMsg.setCustomSelectionActionModeCallback(new ActionMode.Callback2() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater menuInflater = mode.getMenuInflater();
                    if (menuInflater != null) {
                        menuInflater.inflate(R.menu.popup_menu, menu);
                    }
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

                    switch (item.getItemId()) {
                        case R.id.func_delete:
                            mMessageBeans.remove(position);
                            notifyDataSetChanged();
                            break;
                    }

                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {

                }
            });

        } else if (holder instanceof LeftImgInnerHolder) {
            if (mMessageBeans.get(position).filePath != null) {
                ((LeftImgInnerHolder) holder).setData(mMessageBeans.get(position), mContext);
                //点击图片放大
                ((LeftImgInnerHolder) holder).mImgPhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showImageDialog(mMessageBeans.get(position), position);
                    }
                });
            }
        } else if (holder instanceof RightImgInnerHolder) {
            if (mMessageBeans.get(position).filePath != null) {
                ((RightImgInnerHolder) holder).setData(mMessageBeans.get(position), mContext);
                //点击图片放大
                ((RightImgInnerHolder) holder).mImgPhoto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showImageDialog(mMessageBeans.get(position), position);
                    }
                });
            }
        } else if (holder instanceof LeftFileInnerHolder) {
            ((LeftFileInnerHolder) holder).setData(mMessageBeans.get(position), mContext);
            ((LeftFileInnerHolder) holder).mOpenFile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFileByPath(mMessageBeans.get(position).filePath);
                }
            });

        } else if (holder instanceof RightFileInnerHolder) {
            ((RightFileInnerHolder) holder).setData(mMessageBeans.get(position), mContext);
            ((RightFileInnerHolder) holder).mOpenFile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openFileByPath(mMessageBeans.get(position).filePath);
                }
            });
        } else if ((holder instanceof LeftAudioInnerHolder)) {
            ((LeftAudioInnerHolder) holder).setData(mMessageBeans.get(position), mContext);
            ((LeftAudioInnerHolder) holder).mAudioLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageView imgAudio = ((LeftAudioInnerHolder) holder).mAudioLayout.findViewById(R.id.img_audio);
                    imgAudio.setImageResource(R.drawable.audio_color_change);
                    AnimationDrawable drawable = (AnimationDrawable) imgAudio.getDrawable();
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    File file = new File(mMessageBeans.get(position).filePath);
                    try {
                        mediaPlayer.setDataSource(file.getPath());
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mediaPlayer.start();
                                drawable.start();
                            }
                        });

                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.stop();
                                mp.reset();
                                mp.release();
                                drawable.stop();
                                imgAudio.setImageResource(R.drawable.audio);
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            ((RightAudioInnerHolder) holder).setData(mMessageBeans.get(position), mContext);


            ((RightAudioInnerHolder) holder).mAudioLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    File file = new File(mMessageBeans.get(position).filePath);
                    ImageView imgAudio = ((RightAudioInnerHolder) holder).mAudioLayout.findViewById(R.id.img_audio);
                    imgAudio.setImageResource(R.drawable.audio_color_change);
                    AnimationDrawable drawable = (AnimationDrawable) imgAudio.getDrawable();

                    try {
                        mediaPlayer.setDataSource(file.getPath());
                        mediaPlayer.prepareAsync();
                        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mediaPlayer.start();
                                drawable.start();
                            }
                        });

                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mp.stop();
                                mp.reset();
                                mp.release();
                                drawable.stop();
                                imgAudio.setImageResource(R.drawable.audio);
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }

    /**
     * 列表的项数
     * @return 返回消息数
     */
    @Override
    public int getItemCount() {
        if (mMessageBeans != null) {
            return mMessageBeans.size();
        }
        return 0;
    }

    /**
     * 显示放大图片的Dialog
     * @param messageBean 消息内容，包括文件路径
     * @param position 消息位置
     */
    public void showImageDialog(MessageBean messageBean, int position) {
        ConstraintLayout view = (ConstraintLayout) LayoutInflater.from(mContext).inflate(R.layout.bottom_dialog_img, null);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mContext.getResources().getDisplayMetrics().widthPixels,
                mContext.getResources().getDisplayMetrics().heightPixels);
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mContext, R.style.BottomSheetDialog);
        bottomSheetDialog.setContentView(view, params);

        ImageView imgBack = view.findViewById(R.id.img_back);
        ImageView imgClose = view.findViewById(R.id.img_close);
        TextView textFilePath = view.findViewById(R.id.show_file_path);
        TextView textFileName = view.findViewById(R.id.show_file_name);
        ImageView imgShow = view.findViewById(R.id.img_show);
        TextView textFileLength = view.findViewById(R.id.show_file_length);

        //设置弹窗初始高为满屏高度
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from((View) view.getParent());
        bottomSheetBehavior.setPeekHeight(mContext.getResources().getDisplayMetrics().heightPixels);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    bottomSheetDialog.dismiss();
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        bottomSheetDialog.show();


        //返回Activity
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        //删除图片
        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                mMessageBeans.remove(position);
                notifyDataSetChanged();
            }
        });

        //显示文件路径
        textFilePath.setText("文件保存位置：" + messageBean.filePath);

        //显示图片大小
        textFileLength.setText(messageBean.fileLength);

        //显示图片名称
        textFileName.setText(messageBean.fileName);

        //展示图片
        Glide.with(view).load(new File(messageBean.filePath)).into(imgShow);

    }

    /**
     * 查看文件
     * @param path 文件路径
     */
    private void openFileByPath(String path) {
        File file = new File(path);
        Log.d(TAG, "openFileByPath: " + file.getAbsolutePath());
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = FileProvider.getUriForFile(mContext, "com.example.starchat.fileprovider", file);
        String type = FileUtil.getMIMEType(file);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(uri, type);
        mContext.startActivity(intent);
    }

    /**
     * 接收的消息ViewHolder
     */
    public static class LeftMsgInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private TextView mTextMsg;
        private ImageView mImgProfile;

        public LeftMsgInnerHolder(@NonNull View itemView) {
            super(itemView);

            mTextTime = itemView.findViewById(R.id.text_others_time);
            mTextName = itemView.findViewById(R.id.text_others_sender_name);
            mTextMsg = itemView.findViewById(R.id.text_others_msg);
            mImgProfile = itemView.findViewById(R.id.text_others_profile);

        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            mTextMsg.setText(messageBean.msg);
            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }
        }
    }

    /**
     * 发送的消息ViewHolder
     */
    public static class RightMsgInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private TextView mTextMsg;
        private ImageView mImgProfile;

        public RightMsgInnerHolder(@NonNull View itemView) {
            super(itemView);

            mTextTime = itemView.findViewById(R.id.text_self_time);
            mTextName = itemView.findViewById(R.id.text_self_sender_name);
            mTextMsg = itemView.findViewById(R.id.text_self_msg);
            mImgProfile = itemView.findViewById(R.id.text_self_profile);

        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            mTextMsg.setText(messageBean.msg);

            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }
        }
    }

    /**
     * 接收的图片ViewHolder
     */
    private static class LeftImgInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private ImageView mImgPhoto;
        private ImageView mImgProfile;

        public LeftImgInnerHolder(View view) {
            super(view);

            mTextTime = view.findViewById(R.id.text_others_time);
            mTextName = view.findViewById(R.id.text_others_sender_name);
            mImgPhoto = view.findViewById(R.id.img_others_sender);
            mImgProfile = view.findViewById(R.id.img_others_profile);

        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            Glide.with(context).load(new File(messageBean.filePath)).override(500).into(mImgPhoto);
            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }
        }
    }

    /**
     * 发送的消息ViewHolder
     */
    private static class RightImgInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private ImageView mImgPhoto;
        private ImageView mImgProfile;

        public RightImgInnerHolder(View view) {
            super(view);

            mTextTime = view.findViewById(R.id.text_self_time);
            mTextName = view.findViewById(R.id.text_self_sender_name);
            mImgPhoto = view.findViewById(R.id.img_self_sender);
            mImgProfile = view.findViewById(R.id.img_self_profile);
        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            Glide.with(context).load(new File(messageBean.filePath)).override(300, 500).into(mImgPhoto);
            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }
        }
    }

    /**
     * 接收的文件ViewHolder
     */
    private static class LeftFileInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private ImageView mImgFile;
        private TextView mTextFileName;
        private TextView mTextFileLength;
        private LinearLayout mOpenFile;
        private ImageView mImgProfile;

        public LeftFileInnerHolder(View view) {
            super(view);

            mTextTime = view.findViewById(R.id.text_others_time);
            mTextName = view.findViewById(R.id.text_others_sender_name);
            mImgFile = view.findViewById(R.id.img_file);
            mTextFileName = view.findViewById(R.id.text_file_name);
            mTextFileLength = view.findViewById(R.id.text_file_length);
            mOpenFile = view.findViewById(R.id.open_file_left);
            mImgProfile = view.findViewById(R.id.img_others_profile);

        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            mImgFile.setImageResource(messageBean.fileType);
            mTextFileName.setText(messageBean.fileName);
            mTextFileLength.setText(messageBean.fileLength);
            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }
        }
    }

    /**
     * 发送的文件ViewHolder
     */
    private static class RightFileInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private ImageView mImgFile;
        private TextView mTextFileName;
        private TextView mTextFileLength;
        private LinearLayout mOpenFile;
        private ImageView mImgProfile;

        public RightFileInnerHolder(View view) {
            super(view);

            mTextTime = view.findViewById(R.id.text_self_time);
            mTextName = view.findViewById(R.id.text_self_sender_name);
            mImgFile = view.findViewById(R.id.img_file);
            mTextFileName = view.findViewById(R.id.text_file_name);
            mTextFileLength = view.findViewById(R.id.text_file_length);
            mOpenFile = view.findViewById(R.id.open_file_right);
            mImgProfile = view.findViewById(R.id.img_self_profile);

        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            mImgFile.setImageResource(messageBean.fileType);
            mTextFileName.setText(messageBean.fileName);
            mTextFileLength.setText(messageBean.fileLength);
            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }
        }
    }

    /**
     * 接收的语音ViewHolder
     */
    private static class LeftAudioInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private TextView mTextAudioLength;
        private ImageView mImgProfile;
        private RelativeLayout mAudioLayout;

        public LeftAudioInnerHolder(View view) {
            super(view);
            mTextTime = view.findViewById(R.id.text_others_time);
            mTextName = view.findViewById(R.id.text_others_sender_name);
            mTextAudioLength = view.findViewById(R.id.time_length);
            mImgProfile = view.findViewById(R.id.text_others_profile);
            mAudioLayout = view.findViewById(R.id.audio_left);
        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            mTextAudioLength.setText(messageBean.audioLength);
            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }

        }

    }

    /**
     * 发送的语音ViewHolder
     */
    private static class RightAudioInnerHolder extends RecyclerView.ViewHolder {

        private TextView mTextTime;
        private TextView mTextName;
        private TextView mTextAudioLength;
        private ImageView mImgProfile;
        private RelativeLayout mAudioLayout;

        public RightAudioInnerHolder(View view) {
            super(view);
            mTextTime = view.findViewById(R.id.text_self_time);
            mTextName = view.findViewById(R.id.text_self_sender_name);
            mTextAudioLength = view.findViewById(R.id.time_length);
            mImgProfile = view.findViewById(R.id.text_self_profile);
            mAudioLayout = view.findViewById(R.id.audio_right);
        }

        public void setData(MessageBean messageBean, Context context) {
            mTextTime.setText(messageBean.time);
            mTextName.setText(messageBean.nickname);
            mTextAudioLength.setText(messageBean.audioLength);

            String path;
            path = messageBean.profilePath;
            if (new File(path).exists()) {
                Glide.with(context)
                        .load(path)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            } else {
                Glide.with(context)
                        .load(R.drawable.profile)
                        .override(500)
                        .fitCenter()
                        .placeholder(R.drawable.profile)
                        .into(mImgProfile);
            }
        }
    }
}
