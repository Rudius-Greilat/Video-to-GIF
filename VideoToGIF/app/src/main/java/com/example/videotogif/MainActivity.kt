import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*

class MainActivity : AppCompatActivity() {

    private lateinit var textInputPath: TextView
    private lateinit var buttonSelectFile: Button
    private lateinit var buttonProcessSave: Button
    private lateinit var textOutputPath: TextView

    private var inputUri: Uri? = null
    private var outputUri: Uri? = null
    private var processedContent: String? = null

    private val selectInputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            inputUri = result.data?.data
            inputUri?.let {
                textInputPath.text = "Input File: ${it.path}"
            } ?: run {
                textInputPath.text = "Input File: Not selected"
            }
        }
    }

    private val saveOutputLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            outputUri = result.data?.data
            outputUri?.let {
                textOutputPath.text = "Output File: ${it.path}"
                // Write the processed content to the output URI
                writeOutputFile(it, processedContent ?: "")
            } ?: run {
                textOutputPath.text = "Output File: Not selected"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textInputPath = findViewById(R.id.textInputPath)
        buttonSelectFile = findViewById(R.id.buttonSelectFile)
        buttonProcessSave = findViewById(R.id.buttonProcessSave)
        textOutputPath = findViewById(R.id.textOutputPath)

        buttonSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
            }
            selectInputLauncher.launch(intent)
        }

        buttonProcessSave.setOnClickListener {
            if (inputUri == null) {
                Toast.makeText(this, "Please select an input file.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Read and process the input file
            processedContent = readAndProcessInputFile(inputUri!!)
            if (processedContent == null) {
                return@setOnClickListener
            }
            // Save the processed content to an output file
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "processed_output.txt")
            }
            saveOutputLauncher.launch(intent)
        }
    }

    private fun readAndProcessInputFile(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            reader.close()
            inputStream.close()
            content.uppercase()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to read or process input file.", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun writeOutputFile(uri: Uri, content: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                outputStream?.use { it.write(content.toByteArray()) }
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "File saved successfully.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to save output file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}