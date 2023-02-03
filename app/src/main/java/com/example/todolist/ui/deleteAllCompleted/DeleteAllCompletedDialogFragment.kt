package com.example.todolist.ui.deleteAllCompleted

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeleteAllCompletedDialogFragment : DialogFragment() {

    @Inject
    lateinit var deleteAllCompletedViewModelFactory: DeleteAllCompletedViewModel.DeleteAllCompletedViewModelFactory

    private val viewModel: DeleteAllCompletedViewModel by viewModels {
        DeleteAllCompletedViewModel.provideFactory(
            deleteAllCompletedViewModelFactory,
            this,
            arguments
        )
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm deletion")
            .setMessage("Do you want delete all completed tasks?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Yes") { _, _ ->
                viewModel.onConfirmClick()
            }
            .create()
}
