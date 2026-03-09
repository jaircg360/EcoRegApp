package com.ecolim.ecoregapp.ui.fragments;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.ecolim.ecoregapp.R;

public class SuccessFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_nuevo_registro).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_success_to_registro));

        view.findViewById(R.id.btn_ir_inicio).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_success_to_home));
    }
}
