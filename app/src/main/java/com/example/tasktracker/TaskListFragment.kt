package com.example.tasktracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskListFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var viewModel: TaskViewModel
    private lateinit var adapter: TaskAdapter
    private var allTasks = listOf<Task>()
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())
        repository = TaskRepository(database.taskDao())
        viewModel = TaskViewModel(repository)

        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAdd)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TaskAdapter { task, isChecked ->
            viewModel.updateTask(task.copy(isCompleted = isChecked))
        }
        recyclerView.adapter = adapter

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText?.lowercase() ?: ""
                filterTasks()
                return true
            }
        })

        fabAdd.setOnClickListener {
            val action = TaskListFragmentDirections.actionTaskListFragmentToAddTaskFragment()
            findNavController().navigate(action)
        }

        lifecycleScope.launch {
            viewModel.activeTasks.collect { tasks ->
                allTasks = tasks
                filterTasks()
            }
        }
    }

    private fun filterTasks() {
        val filtered = if (searchQuery.isBlank()) {
            allTasks
        } else {
            allTasks.filter { task ->
                task.title.lowercase().contains(searchQuery) ||
                        task.description.lowercase().contains(searchQuery) ||
                        task.category.lowercase().contains(searchQuery)
            }
        }
        adapter.submitList(filtered)
    }

    inner class TaskAdapter(
        private val onCheckChanged: (Task, Boolean) -> Unit
    ) : RecyclerView.Adapter<TaskAdapter.ViewHolder>() {

        private var tasks = listOf<Task>()

        fun submitList(list: List<Task>) {
            tasks = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_task, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.title.text = task.title
            holder.description.text = task.description
            holder.info.text = "${task.category} • ${SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(task.dueDate))}"
            holder.checkBox.setOnCheckedChangeListener(null)
            holder.checkBox.isChecked = task.isCompleted
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(task, isChecked)
            }
        }

        override fun getItemCount(): Int = tasks.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val checkBox: CheckBox = view.findViewById(R.id.checkBox)
            val title: TextView = view.findViewById(R.id.tvTitle)
            val description: TextView = view.findViewById(R.id.tvDescription)
            val info: TextView = view.findViewById(R.id.tvInfo)
        }
    }
}