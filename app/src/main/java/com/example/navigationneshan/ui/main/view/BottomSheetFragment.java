package com.example.navigationneshan.ui.main.view;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.example.navigationneshan.R;
import com.example.navigationneshan.databinding.FragmentBottomsheetBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;


public class BottomSheetFragment extends BottomSheetDialogFragment {

    public IStartNavigate iStartNavigate;

    public BottomSheetFragment(IStartNavigate iStartNavigate) {
        this.iStartNavigate = iStartNavigate;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        FragmentBottomsheetBinding binding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                R.layout.fragment_bottomsheet, null, false);
        binding.setMyClickHandler(new MyClickHandler());
        View view = binding.getRoot();

        dialog.setContentView(view);
        ((View) view.getParent()).setBackgroundColor(getResources().getColor(android.R.color.transparent));

        assert getArguments() != null;
        String bundleTextAddress = "مقصد: " + getArguments().getString("address");
        String bundleTextDurationAndDistance = getArguments().getString("distance") + " | " +
                getArguments().getString("duration");

        binding.setCurrentText(bundleTextAddress);
        binding.setDurationAndDistance(bundleTextDurationAndDistance);

    }

    public class MyClickHandler {
        public void startNavigate(View view) {
            dismiss();
            iStartNavigate.start();

        }
    }
}

interface IStartNavigate {
    void start();
}