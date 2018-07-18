package edu.upc.mcia.androidpracticabluetooth.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import edu.upc.mcia.androidpracticabluetooth.R;
import edu.upc.mcia.androidpracticabluetooth.command.BytesCommand;

public class BytesFragment extends Fragment {

    // Constant
    public static final int INITIAL_INDICATORS = 5;
    public static final int INITIAL_CONTROLS = 5;

    // Listener
    private OnBytesFragmentListener listener;

    // Byte indicators and controls
    private LinearLayout indicators;
    private LinearLayout controls;

    // Change send/reception length controls
    private TextView lengthIndicatorsText;
    private TextView lengthControlsText;
    private int lengthIndicators;
    private int lengthControls;

    public static BytesFragment newInstance() {
        BytesFragment fragment = new BytesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public BytesFragment() {
        lengthIndicators = INITIAL_INDICATORS;
        lengthControls = INITIAL_CONTROLS;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bytes, container, false);

        // Build indicators
        indicators = view.findViewById(R.id.linearBytesDisplay);
        lengthIndicatorsText = view.findViewById(R.id.indicatorsLengthText);
        lengthIndicatorsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangeReceptionLengthDialog();
            }
        });
        buildIndicators();

        // Build controls
        controls = view.findViewById(R.id.linearBytesControl);
        lengthControlsText = view.findViewById(R.id.controlsLengthText);
        lengthControlsText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showChangeSendingLengthDialog();
            }
        });
        buildControls();

        // Link button to function
        Button send = view.findViewById(R.id.sendButton);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSendButtonPressed();
            }
        });

        return view;
    }

    public void buildIndicators() {
        indicators.removeAllViews();
        for (int i = 0; i < lengthIndicators; i++) {
            TextView text = new TextView(getActivity());
            text.setText("-");
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(12, 0, 12, 0);
            text.setLayoutParams(params);
            indicators.addView(text);
        }
        displayReceivedCommand(new BytesCommand(lengthIndicators));
        lengthIndicatorsText.setText(Integer.toString(lengthIndicators));
    }

    public void buildControls() {
        controls.removeAllViews();
        TextWatcher controlWatcher = new ByteTextWatcher();
        for (int i = 0; i < lengthControls; i++) {
            EditText text = new EditText(getActivity());
            text.setText("0");
            text.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            text.addTextChangedListener(controlWatcher);
            text.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            controls.addView(text);
        }
        lengthControlsText.setText(Integer.toString(lengthControls));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (OnBytesFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnBytesFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    public void showChangeReceptionLengthDialog() {
        String[] choices = new String[BytesCommand.MAX_LENGTH - BytesCommand.MIN_LENGTH + 1];
        for (int i = BytesCommand.MIN_LENGTH; i <= BytesCommand.MAX_LENGTH; i++) {
            choices[i - BytesCommand.MIN_LENGTH] = Integer.toString(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.bytes_dialogTitle).setItems(choices, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                lengthIndicators = which + BytesCommand.MIN_LENGTH;
                buildIndicators();
                listener.onChangedReceptionLength(lengthIndicators);
            }
        });
        builder.create().show();
    }

    public void showChangeSendingLengthDialog() {
        String[] choices = new String[BytesCommand.MAX_LENGTH - BytesCommand.MIN_LENGTH + 1];
        for (int i = BytesCommand.MIN_LENGTH; i <= BytesCommand.MAX_LENGTH; i++) {
            choices[i - BytesCommand.MIN_LENGTH] = Integer.toString(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.bytes_dialogTitle).setItems(choices, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                lengthControls = which + BytesCommand.MIN_LENGTH;
                buildControls();
            }
        });
        builder.create().show();
    }

    public void onSendButtonPressed() {
        if (listener != null && controls != null) {
            int len = controls.getChildCount();
            BytesCommand command = new BytesCommand(len);
            for (int i = 0; i < len; i++) {
                String text = ((EditText) controls.getChildAt(i)).getText().toString();
                int val = 0;
                try {
                    val = Integer.parseInt(text);
                } catch (NumberFormatException nfe) {
                }
                command.array[i] = val;
            }
            listener.onSendBytesCommand(command);
        }
    }

    public void displayReceivedCommand(BytesCommand command) {
        if (indicators != null) {
            String[] values = command.toStringArray();
            for (int i = 0; (i < indicators.getChildCount()) && (i < values.length); i++) {
                TextView text = (TextView) indicators.getChildAt(i);
                text.setText(values[i]);
            }
        }
    }

    public interface OnBytesFragmentListener {

        public void onSendBytesCommand(BytesCommand command);

        public void onChangedReceptionLength(int receptionLength);

    }

    private static class ByteTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            try {
                int input = Integer.parseInt(s.toString());
                if (input < 0) {
                    s.clear();
                    s.append("0");
                } else if (input > 255) {
                    s.clear();
                    s.append("255");
                } else {
                    // is valid
                }
            } catch (NumberFormatException nfe) {
            }
        }
    }

}
