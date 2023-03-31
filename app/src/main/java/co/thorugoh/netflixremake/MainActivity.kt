package co.thorugoh.netflixremake

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.thorugoh.netflixremake.model.Category
import co.thorugoh.netflixremake.model.Movie
import co.thorugoh.netflixremake.util.CategoryTask

class MainActivity : AppCompatActivity(), CategoryTask.Callback {
    private lateinit var progress: ProgressBar
    private lateinit var adapter: CategoryAdapter
    val categories = mutableListOf<Category>()

    // m-v-c (model - [view/controller] activity)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = findViewById(R.id.progress_main)

        adapter = CategoryAdapter(categories) { id ->
            val intent = Intent(this@MainActivity, MovieActivity::class.java)
            intent.putExtra("id", id)
            startActivity(intent)
        }
        val rv: RecyclerView = findViewById(R.id.rv_main)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        CategoryTask(this).execute("https://api.tiagoaguiar.co/netflixapp/home?apiKey=8f357272-8662-4ee3-aeb4-b409e835cfa3")
    }

    override fun onPreExecute() {
        progress.visibility = View.VISIBLE
    }

    override fun onResult(categories: List<Category>) {
        Log.i("Main Cat", categories.toString())
        this.categories.clear()
        this.categories.addAll(categories)

        this.adapter.notifyDataSetChanged()
        progress.visibility = View.GONE
    }

    override fun onFailure(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        progress.visibility = View.GONE
    }


}