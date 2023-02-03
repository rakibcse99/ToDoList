package com.example.todolist.ui.tasks

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.R
import com.example.todolist.data.SortOrder
import com.example.todolist.data.Task
import com.example.todolist.databinding.FragmentTasksBinding
import com.example.todolist.util.exhaustive
import com.example.todolist.util.onQueryTextChanged
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialElevationScale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskFragment : Fragment(R.layout.fragment_tasks), TasksAdapter.OnItemClickListener {

    @Inject
    lateinit var tasksViewModelFactory: TasksViewModel.TasksViewModelFactory

    private val viewModel: TasksViewModel by viewModels {
        TasksViewModel.provideFactory(tasksViewModelFactory, this, arguments)
    }

    private lateinit var searchView: SearchView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentTasksBinding.bind(view)

        val tasksAdapter = TasksAdapter(this)

        binding.apply {
            recyclerviewTasks.apply {
                adapter = tasksAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }

            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val task = tasksAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTaskSwiped(task)
                }
            }).attachToRecyclerView(recyclerviewTasks)

            fabAddTask.setOnClickListener {
                viewModel.onAddNewTaskClick()
            }
        }

        setFragmentResultListener("add_edit_request") { _, bundle ->
            val result = bundle.getInt("add_edit_result")
            viewModel.onAddEditResult(result)
        }

        viewModel.tasks.observe(viewLifecycleOwner) {
            tasksAdapter.submitList(it)
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.taskEvent.collect { event ->
                when (event) {
                    is TasksViewModel.TasksEvent.ShowUndoDeleteTaskMessage -> {
                        Snackbar.make(requireView(), "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO") {
                                viewModel.onUndoDeleteClick(event.task)
                            }.show()
                    }
                    is TasksViewModel.TasksEvent.NavigateToAddTaskScreen -> {
                        val action = TaskFragmentDirections.actionTaskFragmentToAddEditTaskFragment(
                            null,
                            "New Task"
                        )
                        findNavController().navigate(action)

                    }
                    is TasksViewModel.TasksEvent.NavigateToEditTaskScreen -> {
                        val transitionDestination =
                            requireContext().getString(R.string.task_transition_name)
                        val extras = FragmentNavigatorExtras(
                            event.taskView to transitionDestination,
                        )
                        val action = TaskFragmentDirections.actionTaskFragmentToAddEditTaskFragment(
                            event.task,
                            "Edit Task"
                        )
                        findNavController().navigate(action, extras)
                        setExitReenterTransition()
                    }

                    is TasksViewModel.TasksEvent.ShowTaskSavedConfirmationMessage -> {

                        Snackbar.make(requireView(), event.msg, Snackbar.LENGTH_SHORT).show()
                    }
                    TasksViewModel.TasksEvent.NavigateToDeleteAllCompletedScreen -> {
                        val action =
                            TaskFragmentDirections.actionGlobalDeleteAllCompletedDialogFragment()
                        findNavController().navigate(action)
                    }
                }
            }.exhaustive
        }

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

//        setHasOptionsMenu(true)
    }

    override fun onItemClick(task: Task, taskView: View) {
        viewModel.onTaskSelected(task, taskView)
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
        viewModel.onTaskCheckedChange(task, isChecked)
    }


    private fun setExitReenterTransition() {
        exitTransition = MaterialElevationScale(false).apply {
            duration = 300
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = 300
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        searchView.setOnQueryTextListener(null)
    }
}
