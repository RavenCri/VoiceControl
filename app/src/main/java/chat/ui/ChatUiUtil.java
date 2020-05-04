package chat.ui;

import com.github.bassaer.chatmessageview.model.ChatUser;
import com.github.bassaer.chatmessageview.model.Message;
import com.github.bassaer.chatmessageview.view.ChatView;

import raven.speak.ActivityUiDialog;

public class ChatUiUtil {

    public static ChatUser userRight = ActivityUiDialog.userRight;
    public static ChatUser userLeft = ActivityUiDialog.userLeft;
    public static void showMsg(String orientation,String msg,boolean reFlush){
        boolean left = orientation.equals("left")?true:false;
        ChatUser temp = left?userLeft:userRight;
        Message message = new Message.Builder()
                .setUser(temp)
                .setRight(!left)
                .setText(msg)
                .build();
        ActivityUiDialog.mChatView.setRefreshing(reFlush);
        ActivityUiDialog.mChatView.send(message);
    }
}
