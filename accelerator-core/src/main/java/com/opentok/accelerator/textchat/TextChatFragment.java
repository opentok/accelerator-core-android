package com.opentok.accelerator.textchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.opentok.accelerator.R;
import com.opentok.accelerator.core.listeners.SignalListener;
import com.opentok.accelerator.core.signal.SignalInfo;
import com.opentok.accelerator.core.wrapper.OTAcceleratorSession;
import com.opentok.accelerator.textchat.config.OpenTokConfig;
import com.tokbox.android.logging.OTKAnalytics;
import com.tokbox.android.logging.OTKAnalyticsData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class TextChatFragment extends Fragment implements SignalListener {

    private final static String LOG_TAG = Fragment.class.getSimpleName();

    private final static int MAX_OPENTOK_LENGTH = 8196;
    private final static int MAX_DEFAULT_LENGTH = 1000;
    private final static String DEFAULT_SENDER_ALIAS = "me";
    private final static String SIGNAL_TYPE = "text-chat";

    private RecyclerView mRecyclerView;
    private ViewGroup rootView;
    private ViewGroup mActionBarView;
    private ViewGroup mSendMessageView;
    private EditText mMsgEditText;
    private TextView mTitleBar;
    private ImageButton mCloseBtn;
    private TextView mMsgCharsView;
    private int maxTextLength = MAX_DEFAULT_LENGTH;
    private TextChatListener mListener;
    private String senderId;
    private String senderAlias;
    private HashMap<String, String> senders = new HashMap<>();

    private List<ChatMessage> messagesList = new ArrayList<ChatMessage>();
    private ChatMessagesAdapter mMessageAdapter;

    private OTAcceleratorSession mSession;
    private String mApiKey;

    private OTKAnalyticsData mAnalyticsData;
    private OTKAnalytics mAnalytics;

    /**
     * Monitors state changes in the TextChatFragment.
     */
    public interface TextChatListener {

        /**
         * Invoked when a new text chat message has been sent.
         *
         * @param message The text chat message that was sent.
         */
        void onNewSentMessage(ChatMessage message);

        /**
         * Invoked when a new text chat message has been received.
         *
         * @param message The text chat message that was received.
         */
        void onNewReceivedMessage(ChatMessage message);

        /**
         * Invoked when a text chat error occurs.
         *
         * @param error The error message.
         */
        void onTextChatError(String error);

        /**
         * Invoked when the close button is clicked.
         */
        void onClosed();

        /**
         * Invoked when the text chat is restarted.
         */
        void onRestarted();
    }

    /**
     * Creates a new TextChatFragment instance
     *
     * @param session the opentok session instance
     * @param apiKey  the partner id
     */
    public static TextChatFragment newInstance(OTAcceleratorSession session, String apiKey) {

        if (session == null || apiKey == null || apiKey.trim().length() == 0) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }

        TextChatFragment fragment = new TextChatFragment();
        fragment.senderId = UUID.randomUUID().toString(); //by default
        fragment.senderAlias = DEFAULT_SENDER_ALIAS; // by default
        fragment.mSession = session;
        fragment.mApiKey = apiKey;

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = (ViewGroup) inflater.inflate(R.layout.fragment_text_chat, container, false);

        mMsgEditText = rootView.findViewById(R.id.edit_msg);
        mTitleBar = rootView.findViewById(R.id.titlebar);
        mCloseBtn = rootView.findViewById(R.id.close);
        mActionBarView = rootView.findViewById(R.id.action_bar);
        mSendMessageView = rootView.findViewById(R.id.send_msg);
        mMsgCharsView = rootView.findViewById(R.id.characteres_msg);
        mMsgCharsView.setText(String.valueOf(maxTextLength));
        mMsgEditText.addTextChangedListener(mTextEditorWatcher);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(layoutManager);

        mMsgEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        mMsgEditText.setMaxWidth(displaymetrics.widthPixels - (int) getResources().getDimension(R.dimen.edit_text_width));

        try {
            mMessageAdapter = new ChatMessagesAdapter(messagesList);
            mRecyclerView.setAdapter(mMessageAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, "Close onClick");
                try {
                    InputMethodManager inputMgr = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMgr.hideSoftInputFromWindow(mMsgEditText.getWindowToken(), 0);
                } catch (Exception e) {
                    throw e;
                }
                onClose();
            }
        });

        updateTitle(defaultTitle());

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //Fragment could not be yet visible, but it belongs to a visible parent container
        addLogEvent(OpenTokConfig.LOG_ACTION_OPEN, OpenTokConfig.LOG_VARIATION_ATTEMPT);
        addLogEvent(OpenTokConfig.LOG_ACTION_OPEN, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    /**
     * Close the text chat view.
     */
    public void close() {
        onClose();
    }

    /**
     * Sets a {@link TextChatListener} object to monitor state changes for this
     * TextChatFragment object.
     *
     * @param listener The {@link TextChatListener} instance.
     */
    public void setListener(TextChatListener listener) {
        this.mListener = listener;
    }

    /**
     * Set the maximum length of a text chat message (in characters).
     *
     * @param length The maximum length of a text chat message (in characters).
     */
    public void setMaxTextLength(int length) throws IllegalArgumentException {
        addLogEvent(OpenTokConfig.LOG_ACTION_SET_MAX_LENGTH, OpenTokConfig.LOG_VARIATION_ATTEMPT);
        if (length > MAX_OPENTOK_LENGTH) {
            onError("Your maximum length is over size limit on the OpenTok platform (maximum length 8196)");
            addLogEvent(OpenTokConfig.LOG_ACTION_SET_MAX_LENGTH, OpenTokConfig.LOG_VARIATION_ERROR);
            throw new IllegalArgumentException("The maximum length cannot be over size limit on the OpenTok platform (maximum length 8196)");
        } else {
            if (length <= 0) {
                onError("Your maximum length must be greater than 0");
                throw new IllegalArgumentException("The maximum length must be greater than 0");
            }
            maxTextLength = length;
            mMsgCharsView.setText(String.valueOf(maxTextLength));
            addLogEvent(OpenTokConfig.LOG_ACTION_SET_MAX_LENGTH, OpenTokConfig.LOG_VARIATION_SUCCESS);
        }
    }

    /**
     * Get the max text length
     *
     * @return The max text length.
     */
    public int getMaxTextLength() {
        return maxTextLength;
    }

    /**
     * Set the sender alias for outgoing messages.
     *
     * @param senderAlias The alias for the sender.
     */
    public void setSenderAlias(String senderAlias) throws IllegalArgumentException {

        if (senderAlias == null || senderAlias.length() == 0) {
            onError("The alias cannot be null or empty");
            throw new IllegalArgumentException("Sender allias cannot be null or empty");
        }
        this.senderAlias = senderAlias;
        senders.put(senderId, senderAlias);
        updateTitle(defaultTitle());
    }

    /**
     * Get the sender alias
     *
     * @return The sender alias.
     */
    public String getSenderAlias() {
        return this.senderAlias;
    }

    /**
     * Get the action bar to be customized.
     *
     * @return The action bar.
     */
    public ViewGroup getActionBar() { return mActionBarView; }

    /**
     * Set the customized action bar.
     *
     * @param actionBar The customized action bar.
     */
    public void setActionBar(ViewGroup actionBar) throws IllegalArgumentException {
        if (actionBar == null) {
            throw new IllegalArgumentException("ActionBar cannot be null");
        }
        mActionBarView = actionBar;
    }

    /**
     * Get the send message area view to be customized.
     *
     * @return The send message view.
     */
    public ViewGroup getSendMessageView() {
        return mSendMessageView;
    }

    /**
     * Set the customized send message area view.
     *
     * @param sendMessageView The customized send message area view.
     */
    public void setSendMessageView(ViewGroup sendMessageView) throws IllegalArgumentException {

        if (sendMessageView == null) {
            throw new IllegalArgumentException("MessageView cannot be null");
        }
        mSendMessageView = sendMessageView;
    }

    /**
     * Restart the session, removing all messages and maximizing the view.
     */
    public void restart() {
        messagesList = new ArrayList<ChatMessage>();
        try {
            mMessageAdapter = new ChatMessagesAdapter(messagesList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mRecyclerView.setAdapter(mMessageAdapter);
    }

    /**
     * Internal initialization of the TextChatFragment
     */
    public void init() {
        //internal client logs
        String source = getContext().getPackageName();

        SharedPreferences prefs = getContext().getSharedPreferences("opentok", Context.MODE_PRIVATE);
        String guidVSol = prefs.getString("guidVSol", null);
        if (null == guidVSol) {
            guidVSol = UUID.randomUUID().toString();
            prefs.edit().putString("guidVSol", guidVSol).apply();
        }

        mAnalyticsData = new OTKAnalyticsData.Builder(OpenTokConfig.LOG_CLIENT_VERSION, source, OpenTokConfig.LOG_COMPONENT_ID, guidVSol).build();
        if (mSession.getConnection() != null) {
            //session is connected
            mAnalyticsData.setConnectionId(mSession.getConnection().getConnectionId());
        }
        mAnalytics = new OTKAnalytics(mAnalyticsData);

        mAnalytics.enableConsoleLog(false);
        mAnalyticsData.setPartnerId(mApiKey);
        mAnalytics.setData(mAnalyticsData);

        addLogEvent(OpenTokConfig.LOG_ACTION_INITIALIZE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
        this.mSession.addSignalListener(SIGNAL_TYPE, this);
        addLogEvent(OpenTokConfig.LOG_ACTION_INITIALIZE, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    //Add a message to the message list.
    private void addMessage(final ChatMessage message) throws IllegalArgumentException {
        Log.i(LOG_TAG, "New message " + message.getText() + " is ready to be added.");

        if (message != null) {
            if (!senders.containsKey(message.getSenderId())) {
                senders.put(message.getSenderId(), message.getSenderAlias());
                updateTitle(defaultTitle());
            }

            //generate message timestamp
            Date date = new Date();
            if (message.getTimestamp() == 0) {
                throw new IllegalArgumentException("Timestamp is 0");
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!checkMessageGroup(message)) {
                        messagesList.add(message);
                    } else {
                        //concat text for the messages group
                        String msgText = messagesList.get(messagesList.size() - 1).getText() + "\r\n" + message.getText();


                        try {
                            ChatMessage newMessage = new ChatMessage(
                                    message.getId(),
                                    message.getStatus(),
                                    msgText,
                                    message.getTimestamp(),
                                    message.getSenderId(),
                                    message.getSenderAlias()
                            );

                            Collections.replaceAll(messagesList, message, newMessage);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        messagesList.set(messagesList.size() - 1, message);
                    }

                    mMessageAdapter.notifyDataSetChanged();
                    mRecyclerView.smoothScrollToPosition(mMessageAdapter.getItemCount() - 1); //update based on adapter

                }
            });
        }
    }

    // Called when the user clicks the send button.
    private void sendMessage() {
        //checkMessage
        mMsgEditText.setEnabled(false);
        String msgStr = mMsgEditText.getText().toString();
        if (!msgStr.isEmpty()) {

            if (msgStr.length() > maxTextLength) {
                onError("Your chat message is over size limit");
            } else {
                JSONObject messageObj = new JSONObject();
                JSONObject sender = new JSONObject();

                try {
                    sender.put("id", senderId);
                    sender.put("alias", senderAlias);
                    messageObj.put("sender", sender);
                    messageObj.put("text", msgStr);
                    messageObj.put("sentOn", System.currentTimeMillis());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                addLogEvent(OpenTokConfig.LOG_ACTION_SEND_MESSAGE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                if (mSession.getConnection() != null) {
                    mSession.sendSignal(new SignalInfo(mSession.getConnection().getConnectionId(), null, SIGNAL_TYPE, messageObj.toString()), null);
                } else {
                    Log.i(LOG_TAG, "The session is not connected. You cannot send any messages.");
                }
            }
        } else {
            mMsgEditText.setEnabled(true);
        }
    }

    //Check the time between the current new message and the last added message
    private boolean checkTimeMsg(long lastMsgTime, long newMsgTime) {
        if (lastMsgTime - newMsgTime <= TimeUnit.MINUTES.toMillis(2)) {
            return true;
        }
        return false;
    }

    //Check messages group
    private boolean checkMessageGroup(ChatMessage msg) {
        int size = messagesList.size();
        if (size >= 1) {
            ChatMessage lastAdded = messagesList.get(size - 1);

            //check source
            if (lastAdded.getSenderId().equals(msg.getSenderId())) {
                //check time
                return checkTimeMsg(msg.getTimestamp(), lastAdded.getTimestamp());
            }
        }
        return false;
    }

    //Set title bar
    private String defaultTitle() {
        String title = "";
        Iterator it = senders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            if (!title.isEmpty()) {
                title = title + ", ";
            }
            title = title + e.getValue();
        }
        return title;
    }

    //Update the title bar
    private void updateTitle(final String title) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTitleBar.setText(title);
            }
        });
    }

    //add log events
    private void addLogEvent(String action, String variation) {
        if (mAnalytics != null) {
            mAnalytics.logEvent(action, variation);
        }
    }

    // Count down the characters left.
    private TextWatcher mTextEditorWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            int chars_left = maxTextLength - s.length();

            mMsgCharsView.setText(String.valueOf((maxTextLength - s.length())));
            if (chars_left < 4) {
                mMsgCharsView.setTextColor(Color.RED);

                if (chars_left < 0) {
                    String maxStr = mMsgEditText.getText().toString().substring(0, mMsgEditText.getText().length() - 1);
                    mMsgEditText.setText(maxStr);
                    mMsgEditText.setSelection(mMsgEditText.getText().length());
                }
            } else {
                mMsgCharsView.setTextColor(getResources().getColor(R.color.info));
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    //Text-chat listener events
    protected void onError(String error) {
        if (this.mListener != null) {
            Log.d(LOG_TAG, "onTextChatError");
            this.mListener.onTextChatError(error);
        }
    }

    protected void onClose() {
        addLogEvent(OpenTokConfig.LOG_ACTION_CLOSE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
        if (this.mListener != null) {
            Log.d(LOG_TAG, "onClosed");
            mListener.onClosed();
        }
        addLogEvent(OpenTokConfig.LOG_ACTION_CLOSE, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    protected void onNewSentMessage(ChatMessage message) {
        if (this.mListener != null) {
            Log.d(LOG_TAG, "onNewSentMessage");
            mListener.onNewSentMessage(message);
        }
        addLogEvent(OpenTokConfig.LOG_ACTION_SEND_MESSAGE, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    protected void onNewReceivedMessage(ChatMessage message) {
        if (this.mListener != null) {
            Log.d(LOG_TAG, "onNewReceivedMessage");
            mListener.onNewReceivedMessage(message);
        }
        addLogEvent(OpenTokConfig.LOG_ACTION_RECEIVE_MESSAGE, OpenTokConfig.LOG_VARIATION_SUCCESS);
    }

    protected void onRestart() {
        if (this.mListener != null) {
            Log.d(LOG_TAG, "onRestart");
            mListener.onRestarted();
        }
    }

    @Override
    public void onSignalReceived(SignalInfo signalInfo, boolean isSelfSignal) {
        String senderId = null;
        String senderAlias = null;
        String text = null;
        Long date = null;
        JSONObject json;
        JSONObject sender;

        try {
            if (signalInfo.mData != null && isValid((String) signalInfo.mData)) {
                json = new JSONObject((String) signalInfo.mData);
                if (!json.isNull("text") && !json.isNull("sentOn") && !json.isNull("sender")) {
                    text = json.getString("text");
                    date = json.getLong("sentOn");
                    sender = json.getJSONObject("sender");
                    senderId = sender.getString("id");
                    senderAlias = sender.getString("alias");
                } else {
                    Log.e(LOG_TAG, "The received message has not a supported format.");
                }
            } else {
                Log.e(LOG_TAG, "The received message is not a valid JSON object");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (text == null || text.isEmpty()) {
            onError("Message format is wrong. Text is empty or null");
            if (signalInfo.mSrcConnId.equals(signalInfo.mDstConnId)) {
                addLogEvent(OpenTokConfig.LOG_ACTION_SEND_MESSAGE, OpenTokConfig.LOG_VARIATION_ERROR);
            } else {
                addLogEvent(OpenTokConfig.LOG_ACTION_RECEIVE_MESSAGE, OpenTokConfig.LOG_VARIATION_ERROR);
            }
        } else {
            if (signalInfo.mSrcConnId.equals(signalInfo.mDstConnId)) {
                try {
                    final ChatMessage sentMessage = new ChatMessage(
                            UUID.randomUUID(),
                            ChatMessage.MessageStatus.SENT_MESSAGE,
                            text,
                            date,
                            senderId,
                            senderAlias);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMsgEditText.setEnabled(true);
                            mMsgEditText.setFocusable(true);
                            mMsgEditText.setText("");
                            mMsgCharsView.setTextColor(getResources().getColor(R.color.info));
                            try {
                                addMessage(sentMessage);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    onNewSentMessage(sentMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.i(LOG_TAG, "A new message has been received " + signalInfo.mData);
                addLogEvent(OpenTokConfig.LOG_ACTION_RECEIVE_MESSAGE, OpenTokConfig.LOG_VARIATION_ATTEMPT);
                try {
                    ChatMessage receivedMsg = new ChatMessage(
                            UUID.randomUUID(),
                            ChatMessage.MessageStatus.RECEIVED_MESSAGE,
                            text,
                            date,
                            senderId,
                            senderAlias);

                    addMessage(receivedMsg);
                    onNewReceivedMessage(receivedMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean isValid(String message) {
        try {
            new JSONObject(message);
        } catch (JSONException ex) {
            try {
                new JSONArray(message);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}