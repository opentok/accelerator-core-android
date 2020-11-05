package com.tokbox.accelerator.annotations;

import android.graphics.Paint;
import androidx.annotation.NonNull;

/**
 * Defines an object to be added to the annotations view.
 */
public class Annotatable {

    private AnnotationsView.Mode mode;
    private String data;

    private String cid;
    private AnnotatableType type;
    private AnnotationsPath path;

    private AnnotationsText text;
    private Paint paint;

    /**
     * Defines the type of the objects to be added
     */
    public static enum AnnotatableType {
        PATH,
        TEXT
    }

    /**
     * Constructor
     *
     * @param mode  The AnnotationsView.Mode
     * @param path  The path to be added
     * @param paint the style of the annotation
     * @param cid   the connection id
     */
    public Annotatable(@NonNull AnnotationsView.Mode mode, @NonNull AnnotationsPath path, @NonNull Paint paint,
                       @NonNull String cid) {
        this.cid = cid;
        this.mode = mode;
        this.path = path;
        this.paint = paint;
    }

    /**
     * Constructor
     *
     * @param mode  The AnnotationsView.Mode
     * @param text  The text to be added
     * @param paint the style of the annotation
     * @param cid   the connection id
     */
    public Annotatable(@NonNull AnnotationsView.Mode mode, @NonNull AnnotationsText text, @NonNull Paint paint,
                       @NonNull String cid) {
        this.cid = cid;
        this.mode = mode;
        this.text = text;
        this.paint = paint;
    }

    /**
     * Sets the AnnotationsView.Mode of the annotatable object
     *
     * @param mode
     * @throws Exception
     */
    public void setMode(@NonNull AnnotationsView.Mode mode) {
        this.mode = mode;
    }

    /**
     * Sets the data of the annotatable object
     *
     * @param data
     * @throws Exception
     */
    public void setData(@NonNull String data) {
        this.data = data;
    }

    /**
     * Sets the AnnotatableType of the annotatable object
     *
     * @param type
     */
    public void setType(AnnotatableType type) {
        this.type = type;
    }

    /**
     * Returns the AnnotationsView.Mode
     */
    public AnnotationsView.Mode getMode() {
        return mode;
    }

    /**
     * Returns the data
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the AnnotationsPach
     */
    public AnnotationsPath getPath() {
        return path;
    }

    /**
     * Returns the AnnotatableType
     */
    public AnnotatableType getType() {
        return type;
    }

    /**
     * Returns the connectionId
     */
    public String getCId() {
        return cid;
    }

    /**
     * Returns the AnnotationsText
     */
    public AnnotationsText getText() {
        return text;
    }

    /**
     * Returns the Paint
     */
    public Paint getPaint() {
        return paint;
    }

}

