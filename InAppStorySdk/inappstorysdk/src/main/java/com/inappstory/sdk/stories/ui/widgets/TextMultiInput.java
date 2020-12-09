package com.inappstory.sdk.stories.ui.widgets;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;

import com.inappstory.sdk.stories.utils.PhoneFormats;
import com.inappstory.sdk.stories.utils.Sizes;

public class TextMultiInput extends LinearLayout {
    public TextMultiInput(Context context) {
        super(context);
    }

    public int getMaskLength() {
        if (watcher != null) {
            return watcher.mMask.length();
        }
        return 0;
    }

    public void setHint(String hint) {
        if (inputType == PHONE) {
            phoneNumberHint.setHint(hint);
        } else {
            getMainText().setHint(hint);
        }
        baseHint = hint;
    }

    private String baseHint = "";

    public void setTextColor(int textColor) {
        getMainText().setTextColor(textColor);
        if (inputType == PHONE)
            getCountryCodeText().setTextColor(textColor);
    }


    public String getText() {
        if (inputType == PHONE) {
            return getCountryCodeText().getEditableText().toString() + " " +
                    getMainText().getEditableText().toString();
        } else {
            return getMainText().getEditableText().toString();
        }
    }

    public void setHintTextColor(int hintColor) {
        getMainText().setHintTextColor(hintColor);
        if (inputType == PHONE) {
            phoneNumberHint.setHintTextColor(hintColor);
        }

    }

    public void setTextSize(int size) {
        if (inputType == PHONE) {
            getCountryCodeText().setTextSize(size);
            phoneNumberHint.setTextSize(size);
        }
        getMainText().setTextSize(size);
    }

    public static final String PHONE_CODE_MASK = "+−−−−";

    public AppCompatEditText getMainText() {
        return mainText;
    }

    AppCompatEditText mainText;

    public AppCompatEditText getCountryCodeText() {
        return countryCodeText;
    }

    AppCompatEditText countryCodeText;

    AppCompatEditText phoneNumberHint;

    public static final int PHONE = 0;
    public static final int MAIL = 1;
    public static final int TEXT = 2;

    public int inputType;
    String mask;

    public void init(int inputType) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        this.inputType = inputType;
        mainText = new AppCompatEditText(getContext());
        LayoutParams mainTextLp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        mainTextLp.setMargins(0, 0, 0, 0);
        mainText.setBackground(null);
        mainText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        switch (inputType) {
            case MAIL:
                mainText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                mainText.setGravity(Gravity.CENTER);
                mainTextLp.setMargins(Sizes.dpToPxExt(4), 0, Sizes.dpToPxExt(4), 0);
                mainText.setLayoutParams(mainTextLp);
                mainText.setSingleLine(true);
                mainText.setMaxLines(1);
                addView(mainText);
                break;
            case TEXT:
                mainText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                mainText.setSingleLine(false);
                mainText.setMaxLines(3);
                mainText.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
                mainTextLp.setMargins(Sizes.dpToPxExt(4), 0, Sizes.dpToPxExt(4), 0);
                mainText.setLayoutParams(mainTextLp);
                addView(mainText);
                break;
            case PHONE:
                mainTextLp.setMargins(Sizes.dpToPxExt(4), 0, 0, 0);
                countryCodeText = new AppCompatEditText(getContext());
                LayoutParams lp2 = new LayoutParams(Sizes.dpToPxExt(60),
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp2.setMargins(0, 0, Sizes.dpToPxExt(4), 0);
                divider = new View(getContext());
                divider.setLayoutParams(new ViewGroup.LayoutParams(Sizes.dpToPxExt(1),
                        Sizes.dpToPxExt(30)));
                RelativeLayout rl = new RelativeLayout(getContext());
                rl.setLayoutParams(mainTextLp);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                phoneNumberHint = new AppCompatEditText(getContext());
                countryCodeText.setLayoutParams(lp2);
                countryCodeText.setBackground(null);
                countryCodeText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
                countryCodeText.setInputType(InputType.TYPE_CLASS_PHONE);
                countryCodeText.setInputType(InputType.TYPE_CLASS_PHONE);

                countryCodeText.setGravity(Gravity.CENTER);
                countryCodeText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                countryCodeText.addTextChangedListener(new MaskedWatcher(PHONE_CODE_MASK, "+"));
                countryCodeText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        try {
                            watcher.active = false;
                            mainText.removeTextChangedListener(watcher);
                            watcher = null;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        final CharSequence fs = s;
                        mask = PhoneFormats.getMaskByCode(fs.toString());
                        if (mask != null) {
                            mainText.setHint("");
                            String text = mainText.getText().toString();
                            mainText.setText("");
                            watcher = new MaskedWatcher(mask, "");
                            mainText.addTextChangedListener(watcher);
                            phoneNumberHint.setHint(mask);


                            text = text.replaceAll(" ", "");
                            mainText.setText(text);
                            watcher.afterTextChanged(mainText.getEditableText());

                            mainText.setInputType(InputType.TYPE_CLASS_PHONE);
                        } else {
                            String text = mainText.getText().toString();
                            text = text.replaceAll(" ", "");
                            mainText.setText(text);
                         /*   if (mainText.getText().toString().isEmpty()) {
                                phoneNumberHint.setHint("");
                            } else {
                                phoneNumberHint.setHint(mainText.getText().toString());
                            }*/
                            if (countryCodeText.getText().length() == 1) {
                                mainText.setHint(baseHint);
                            } else {
                                mainText.setHint("");
                            }

                            mainText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        }

                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                countryCodeText.setOnFocusChangeListener(new OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view1, boolean b) {
                        if (!b) return;
                        if (countryCodeText.getText().toString().length() < 1) {
                            countryCodeText.setText("+");
                            countryCodeText.post(new Runnable() {
                                @Override
                                public void run() {
                                    countryCodeText.setSelection(1);
                                }
                            });
                        }
                    }
                });
                mainText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {


                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (mask != null) {
                            if (s.length() <= mask.length()) {
                                String s0 = s + mask.substring(s.length());
                                phoneNumberHint.setHint(s0);
                            }
                        } else {
                            if (s.length() < 20) {
                                phoneNumberHint.setHint(s);
                            } else {
                                phoneNumberHint.setHint("");
                            }
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                });
                phoneNumberHint.setBackground(null);
                phoneNumberHint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                   // mainText.setElevation(8);
                }
                mainText.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                phoneNumberHint.setClickable(false);
                phoneNumberHint.setFocusable(false);
                phoneNumberHint.setLayoutParams(rlp);
                mainText.setSingleLine(true);
                mainText.setMaxLines(1);
                phoneNumberHint.setSingleLine(true);
                phoneNumberHint.setMaxLines(1);

                rl.addView(phoneNumberHint);
                rl.addView(mainText);
                addView(countryCodeText);
                addView(divider);
                addView(rl);
                break;
        }
    }

    MaskedWatcher watcher;

    public View getDivider() {
        return divider;
    }

    View divider;

    public TextMultiInput(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TextMultiInput(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
