package com.tokbox.android.otsdkwrapper.utils;


import android.util.Log;

import com.tokbox.android.otsdkwrapper.GlobalLogLevel;

/**
 * Defines the OpenTok Configuration to be used in the communication.
 */
public class OTConfig {

    private static final String LOG_TAG = OTConfig.class.getSimpleName();
    private static final short LOCAL_LOG_LEVEL = 0xFF;
    private static final LogWrapper LOG =
      new LogWrapper((short)(GlobalLogLevel.sMaxLogLevel & LOCAL_LOG_LEVEL));

    public static void setLogLevel(short logLevel) {
        LOG.setLogLevel(logLevel);
    }
    
    private static final int MAX_LENGTH_NAME = 50;

    String sessionId; //required
    String token; //required
    String apiKey; //required
    String name; //optional
    boolean subscribeToSelf = false; //optional
    boolean subscribeAutomatically = true; //optional

    /**
     * Creates a new OTConfig instance using a builder pattern.
     * @param builder
     */
    public OTConfig(OTConfigBuilder builder) {
        this.sessionId = builder.sessionId;
        this.token = builder.token;
        this.apiKey = builder.apiKey;
        this.name = builder.name;
        this.subscribeAutomatically = builder.subscribeAutomatically;
        this.subscribeToSelf = builder.subscribeToSelf;
    }

    /**
     * Returns the OpenTok sessionId
     * @return the OpenTok sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the OpenTok token
     * @return the OpenTok token
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the OpenTok apiKey
     * @return the OpenTok apiKey
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Returns the name of the OpenTok session
     * @return the OpenTok name session
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the subscribe to self status
     * @return Whether to subscribe to self (<code>true</code>) or not (
     *                     <code>false</code>).
     */
    public boolean shouldSubscribeToSelf(){
        return subscribeToSelf;
    }

    /**
     * Returns the subscribe automatically status
     * @return Whether to subscribe automatically (<code>true</code>) or not (
     *                     <code>false</code>).
     */
    public boolean shouldSubscribeAutomatically(){
        return subscribeAutomatically;
    }

    /**
     * Defines the OTConfig builder pattern
     */
    public static class OTConfigBuilder {

        String sessionId; //required
        String token; //required
        String apiKey; //required

        String name; //optional
        boolean subscribeToSelf; //optional
        boolean subscribeAutomatically = true; //optional

        public OTConfigBuilder(String sessionId, String token, String apikey) {
            if ( sessionId == null || token == null || apikey == null ) {
                throw new RuntimeException("The credentials cannot be null");
            }
            this.sessionId = sessionId;
            this.token = token;
            this.apiKey = apikey;
        }

        public OTConfigBuilder name(String name) {
            if ( name.length() > MAX_LENGTH_NAME ){
                throw new RuntimeException("Name string cannot be greater than "+MAX_LENGTH_NAME);
            }
            else {
                if ( name == null || name.length() == 0 || name.trim().length() == 0 ){
                    throw new RuntimeException("Name cannot be null or empty");
                }
            }
            this.name = name;
            return this;
        }

        public OTConfigBuilder subscribeToSelf(Boolean subscribeToSelf) {
            this.subscribeToSelf = subscribeToSelf;
            return this;
        }

        public OTConfigBuilder subscribeAutomatically(Boolean subscribeAutomatically) {
            this.subscribeAutomatically = subscribeAutomatically;
            return this;
        }

        public OTConfig build() {
            OTConfig info = new OTConfig(this);
            boolean valid = validateInfoObject(info);

            if (!valid) {
                return null;
            }
            return info;
        }

        private boolean validateInfoObject(OTConfig info) {
            if ( sessionId == null || sessionId.isEmpty() || sessionId.trim().length() == 0 ) {
                LOG.i(LOG_TAG, "SessionId cannot be null or empty");
                return false;
            }
            if ( token == null || token.isEmpty() || token.trim().length() == 0 ) {
                LOG.i(LOG_TAG, "Token cannot be null or empty");
                return false;
            }
            if ( apiKey == null || apiKey.isEmpty() || apiKey.trim().length() == 0 ) {
                LOG.i(LOG_TAG, "ApiKey cannot be null or empty");
                return false;
            }
            return true;
        }
    }
}