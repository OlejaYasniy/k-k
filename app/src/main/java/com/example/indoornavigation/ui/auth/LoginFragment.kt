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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.example.indoornavigation.R
import com.example.indoornavigation.ui.common.UiState
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: AuthViewModel by activityViewModels {
        AuthViewModel.Factory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        
        viewModel.resetAuthState()

        val etEmail    = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin   = view.findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = view.findViewById<MaterialButton>(R.id.tvGoRegister)
        val progress   = view.findViewById<ProgressBar>(R.id.progressBar)

        btnLogin.setOnClickListener {
            viewModel.login(
                etEmail.text.toString().trim(),
                etPassword.text.toString()
            )
        }

        tvRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                progress.isVisible = state is UiState.Loading
                when (state) {
                    is UiState.Success -> {
                        viewModel.resetAuthState()
                        
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