package com.example.contactapp

import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update

import kotlinx.coroutines.*
import com.example.contactapp.ui.theme.ContactAppTheme


data class Contact(val id: Long, val name: String, val phoneNumber: String)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone_number") val phoneNumber: String
)
@Dao
interface ContactDao {
    @Insert
    suspend fun insert(contact: ContactEntity)

    @Query("SELECT * FROM contacts")
    fun getAllContacts(): LiveData<List<ContactEntity>>

    @Update
    suspend fun update(contact: ContactEntity)

    @Delete
    suspend fun delete(contact: ContactEntity)
}

@Database(entities = [ContactEntity::class], version = 1, exportSchema = true)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    companion object {
        @Volatile
        private var Instance: ContactDatabase? = null

        fun getDatabase(context: Context): ContactDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, ContactDatabase::class.java, "item_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}

class ContactRepository(private val contactDao: ContactDao) {
    val allContacts: LiveData<List<ContactEntity>> = contactDao.getAllContacts()
    suspend fun insert(contact: ContactEntity) {
        contactDao.insert(contact)
    }

    suspend fun update(contact: ContactEntity) {
        contactDao.update(contact)
    }

    suspend fun delete(contact: ContactEntity) {
        contactDao.delete(contact)
    }

}

class ContactViewModel(application: Application): AndroidViewModel(application) {
    private val repository: ContactRepository
    val allContacts: LiveData<List<ContactEntity>>

    init {
        val contactDao = ContactDatabase.getDatabase(application).contactDao()
        repository = ContactRepository(contactDao)
        allContacts = repository.allContacts
    }

    fun insert(contact: ContactEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(contact)
    }

    fun update(contact: ContactEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(contact)
    }

    fun delete(contact: ContactEntity) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(contact)
    }

    companion object {
        private const val READ_CONTACTS_REQUEST_CODE = 100
    }
}

@ExperimentalMaterial3Api
@Composable
fun ContactAppPage(navController: NavController, contactViewModel: ContactViewModel) {
    // Use remember to keep the state across recompositions
    val contactsentList by contactViewModel.allContacts.observeAsState(initial = emptyList())
    val contactsList = contactsentList.map { contactEntity ->
        Contact(
            id = contactEntity.id,
            name = contactEntity.name,
            phoneNumber = contactEntity.phoneNumber
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Contact App")
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    // Navigate to the ImportContactsActivity
                    navController.navigate("importContacts")
                },
                icon = { Icon(Icons.Filled.Add, "Import Contacts button.") },
                text = { Text(text = "Import Contacts") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding -> LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            items(contactsList) { contact -> contactCard(contact = contact)}
        }
    }
}

@Composable
fun contactCard(contact: Contact) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(text = contact.name)
            Text(text = contact.phoneNumber)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable

fun AppNavHost(navController: NavHostController) {
    // Create Room database instance
    val contactDatabase = Room.databaseBuilder(
        LocalContext.current.applicationContext,
        ContactDatabase::class.java, "contact-database"
    ).build()

// Create ContactDao instance from Room database
    val contactDao = contactDatabase.contactDao()

    val contactViewModel: ContactViewModel = viewModel()
    NavHost(navController = navController, startDestination = "contactAppPage") {
        composable("contactAppPage") {
            ContactAppPage(navController = navController, contactViewModel = contactViewModel)
        }
        composable("importContacts") {
            ImportContactsActivity(navController = navController, contactDao = contactDao, contactViewModel = contactViewModel)
        }
    }
}
@SuppressLint("Range")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsActivity(navController: NavController, contactDao: ContactDao, contactViewModel: ContactViewModel) {
    val contentResolver = LocalContext.current.contentResolver
    val cursor =
        contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

    val tempContacts = emptyList<Contact>().toMutableList()
    if (cursor != null && cursor.count > 0) {
        while (cursor.moveToNext()) {
            val contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID))
            val name =
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
            val phoneNumber =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

            tempContacts += Contact(contactId, name, phoneNumber)

            // Logic to insert contacts into the Room database
            tempContacts.forEach { contact ->
                val contactEntity = ContactEntity(
                    name = contact.name,
                    phoneNumber = contact.phoneNumber
                )
                contactViewModel.insert(contactEntity)
            }
            // Logic to handle checkbox selection and update tempContacts
            val onCheckboxClick: (Contact) -> Unit = { contact ->
                // Implement logic to toggle selection status of the contact
            }
            Scaffold(
                topBar = {
                    // TopAppBar with "Save," "Update," and "Delete" buttons
                },
                content = { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxWidth()
                    ) {
                        // Display tempContacts with select/unselect options
                        items(tempContacts) { contact ->
                            contactCard(contact = contact)
                            // Add checkbox to select/unselect contact
                            // Update tempContacts list accordingly
                            // Pass the selected contact to the click listener
                            // to handle update and delete operations
                        }
                    }
                }
            )
        }
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ContactAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        // Permission granted, proceed with reading contacts
                        AppNavHost(navController = rememberNavController())
                    } else {
                        // Request permission
                        val READ_CONTACTS_REQUEST_CODE = 100
                        requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), READ_CONTACTS_REQUEST_CODE)
                    }

                }
            }
        }
    }
}