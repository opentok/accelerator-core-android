package com.tokbox.android.annotations;

import android.view.View;
import android.widget.EditText;


/**
 * Defines the annotations view text
 */
public class AnnotationsText implements View.OnClickListener{


    EditText editText;

    float x, y;

    /**
     * Constructor
     */
    public AnnotationsText() {

    }

    /**
     * Constructor
     *
     * @param editText Text view editable
     * @param x        x-position
     * @param y        y-position
     */
    public AnnotationsText(EditText editText, float x, float y) {
        this.editText = editText;
        this.x = x;
        this.y = y;
    }

    public void setEditText(EditText editText) {
        this.editText = editText;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    /**
     * Returns the text view editable
     */
    public EditText getEditText() {
        return editText;
    }

    /**
     * Returns the x-position
     */
    public float getX() {
        return x;
    }

    /**
     * Returns the y-position
     */
    public float getY() {
        return y;
    }

    @Override
    public void onClick(View v) {

    }


}
