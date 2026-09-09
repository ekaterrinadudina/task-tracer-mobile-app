package com.example.tasktracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ArchivedTasksFragment : Fragment() {

    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRepository
    private lateinit var viewModel: TaskViewModel
    private lateinit var adapter: ArchivedTaskAdapter
    private var allArchivedTasks = listOf<Task>()
    private var selectedCategory = "Все"
    private var selectedPriority = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_archived_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getInstance(requireContext())
        repository = TaskRepository(database.taskDao())
        viewModel = TaskViewModel(repository)

        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategoryFilter)
        val spinnerPriority = view.findViewById<Spinner>(R.id.spinnerPriorityFilter)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewArchived)

        val categories = arrayOf("Все", "Работа", "Дом", "Учёба")
        val priorities = arrayOf("Все", "Низкий", "Средний", "Высокий")

        spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        spinnerPriority.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, priorities)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = ArchivedTaskAdapter { task ->
            viewModel.updateTask(task.copy(isCompleted = false))
        }
        recyclerView.adapter = adapter

        val onFilterChange = {
            filterTasks()
        }

        spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = if (position == 0) "Все" else categories[position]
                filterTasks()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        spinnerPriority.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPriority = position - 1
                filterTasks()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        lifecycleScope.launch {
            viewModel.archivedTasks.collect { tasks ->
                allArchivedTasks = tasks
                filterTasks()
            }
        }
    }

    private fun filterTasks() {
        val filtered = allArchivedTasks.filter { task ->
            val categoryMatch = selectedCategory == "Все" || task.category == selectedCategory
            val priorityMatch = selectedPriority == -1 || task.priority == selectedPriority
            categoryMatch && priorityMatch
        }
        adapter.submitList(filtered)
    }

    inner class ArchivedTaskAdapter(
        private val onRestore: (Task) -> Unit
    ) : RecyclerView.Adapter<ArchivedTaskAdapter.ViewHolder>() {

        private var tasks = listOf<Task>()

        fun submitList(list: List<Task>) {
            tasks = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_archived_task, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val task = tasks[position]
            holder.title.text = task.title
            holder.info.text = "${task.category} • ${when (task.priority) {
                0 -> "Низкий"
                1 -> "Средний"
                2 -> "Высокий"
                else -> ""
            }} • ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(task.dueDate))}"
            holder.btnRestore.setOnClickListener {
                onRestore(task)
            }
        }

        override fun getItemCount(): Int = tasks.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.tvTitle)
            val info: TextView = view.findViewById(R.id.tvInfo)
            val btnRestore: Button = view.findViewById(R.id.btnRestore)
        }
    }
}