package com.example.indoornavigation.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.example.indoornavigation.MainActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.indoornavigation.R
import com.example.indoornavigation.ui.common.UiState
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private val viewModel: AuthViewModel by activityViewModels {
        AuthViewModel.Factory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        
        viewModel.resetAuthState()

        val etUsername = view.findViewById<TextInputEditText>(R.id.etUsername)
        val etEmail    = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnReg     = view.findViewById<MaterialButton>(R.id.btnRegister)
        val tvLogin    = view.findViewById<MaterialButton>(R.id.tvGoLogin)
        val progress   = view.findViewById<ProgressBar>(R.id.progressBar)

        btnReg.setOnClickListener {
            viewModel.registerDirect(
                etUsername.text.toString().trim(),
                etEmail.text.toString().trim(),
                etPassword.text.toString()
            )
        }

        tvLogin.setOnClickListener { parentFragmentManager.popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                progress.isVisible = state is UiState.Loading
                when (state) {
                    is UiState.Success -> {
                        Toast.makeText(requireContext(),
                            R.string.auth_welcome, Toast.LENGTH_SHORT).show()
                        viewModel.resetAuthState()
                        parentFragmentManager.popBackStack(null,
                            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        (requireActivity() as? MainActivity)?.onAuthSuccess()
                    }
                    is UiState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        viewModel.resetAuthState() 
                    }
                    else -> {}
                }
            }
        }
    }
}