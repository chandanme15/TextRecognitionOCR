package com.example.textrecognition.activities;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.textrecognition.R;
import com.example.textrecognition.utils.TempString;


public class ConfirmationActivity extends AppCompatActivity {

    String text;
    Integer n,i;
    Button btn_manual_input,btn_scan_again;
    TextView vehicleNo;
    Button btn_confirm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);
        vehicleNo = (TextView) findViewById(R.id.vehicleNo);
        btn_manual_input = (Button) findViewById(R.id.btn_manual_input);
        btn_scan_again = (Button) findViewById(R.id.btn_scan_again);
        btn_confirm = (Button) findViewById(R.id.btn_confirm);
       /* CUIHelper.setButtonSelector(btn_confirm,spHelper);
        CUIHelper.setButtonSelector(btn_scan_again,spHelper);
        CUIHelper.setButtonSelector(btn_manual_input,spHelper);*/
        text = TempString.getInstance().getText();
        vehicleNo.setText(text);
        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // triggerVisionResponse(text);
            }
        });
        btn_manual_input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // TransactionHUB.bIsIrisRequest = true;
                /*Intent intent = new Intent(ConfirmationActivity.this, CDisplayDataEntry.class);
                intent.putExtra("TITLE", "Enter Vehicle Num");
                intent.putExtra("MAXLEN", 12);
                intent.putExtra("MINLEN", 0);
                intent.putExtra("InputTpye", 1);
                startActivity(intent);
                finish();*/
            }
        });

        btn_scan_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ConfirmationActivity.this, CameraActivity.class));
                finish();
            }
        });


    }

    /*public void triggerVisionResponse(String VisionData){
        if(TransactionHUB.bIsIrisRequest) {
            VisionData = VisionData.replaceAll("\\s+","");
            if(VisionData.length()>16){
                VisionData = VisionData.substring(0,15);
            }
            IrisPadComm.GetInstance().sendMessage(IrisPadComm.RESPONSE_FOR_OCR_TO_IRIS, 1, 0, VisionData);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if(TransactionHUB.bIsIrisRequest)
            IrisPadComm.GetInstance().sendMessage(IrisPadComm.RESPONSE_FOR_OCR_TO_IRIS, 0, 0, 0);
        finish();
    }*/
}

