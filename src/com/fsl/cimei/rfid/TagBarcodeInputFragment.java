package com.fsl.cimei.rfid;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TagBarcodeInputFragment extends Fragment {
	
	private TextView alotTextView;
	private EditText tagBarcodeInput;
	private Button n7ScanBarcode;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.tag_barcode_input, container);
		alotTextView = (TextView) view.findViewById(R.id.tb_fragment_lot_number);
		tagBarcodeInput = (EditText) view.findViewById(R.id.tb_fragment_input);
		n7ScanBarcode = (Button) view.findViewById(R.id.tb_fragment_n7_scan_barcode);
		return view;
//		return super.onCreateView(inflater, container, savedInstanceState);
	}

	public TextView getAlotTextView() {
		return alotTextView;
	}

	public EditText getTagBarcodeInput() {
		return tagBarcodeInput;
	}
	
	public Button getN7ScanBarcode() {
		return n7ScanBarcode;
	}
}
