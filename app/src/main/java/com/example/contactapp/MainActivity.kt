package com.example.contactapp

import android.app.Application
import android.content.Context
import android.os.Bundle
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.room.*
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

@Database(entities = [ContactEntity::class], version = 1)
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
    val AllContacts: LiveData<List<ContactEntity>> = contactDao.getAllContacts()
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
        allContacts = repository.AllContacts
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
}

@ExperimentalMaterial3Api
@Composable
fun ContactAppPage(navController: NavController, contactDao: ContactDao) {
    // Use remember to keep the state across recompositions
    var contactslist by remember { mutableStateOf(emptyList<Contact>()) }

    // read contacts from room database
    val contactViewModel: ContactViewModel = viewModel()
    contactViewModel.allContacts.observeForever { contacts ->
        contactslist = contacts.map { contactEntity ->
            Contact(
                id = contactEntity.id,
                name = contactEntity.name,
                phoneNumber = contactEntity.phoneNumber
            )
        }
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            // Display contacts in LazyColumn
            items(contactslist) { contact ->
                contactCard(contact = contact)
            }
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

fun AppNavHost(    modifier: Modifier = Modifier,
                   navController: NavHostController,
                   context: Context
) {


    // Create Room database instance
    val contactDatabase = Room.databaseBuilder(
        context.applicationContext,
        ContactDatabase::class.java, "contact-database"
    ).build()

    // Create ContactDao instance from Room database
    val contactDao = contactDatabase.contactDao()
    NavHost(navController = navController, startDestination = "contactAppPage") {
        composable("contactAppPage") {
            ContactAppPage(navController = navController, contactDao = contactDao)
        }
        composable("importContacts") {
            ImportContactsActivity(navController = navController, contactDao = contactDao)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsActivity(navController: NavController, contactDao: ContactDao) {
    // Use remember to keep the state across recompositions
    var tempContacts by remember { mutableStateOf(emptyList<Contact>()) }

    // Logic to read contacts from the content resolver and add them to tempContacts

    // Logic to handle checkbox selection and update tempContacts
    val onCheckboxClick: (Contact) -> Unit = { contact ->
        // Implement logic to toggle selection status of the contact
    }

    Scaffold(
        topBar = {
            // TopAppBar with "Save," "Update," and "Delete" buttons
        },
        content = {innerPadding ->
            LazyColumn (modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()){

                // Display tempContacts with select/unselect options
                items(tempContacts) { contact ->
                    // Add checkbox to select/unselect contact
                    // Update tempContacts list accordingly
                    // Pass the selected contact to the click listener
                    // to handle update and delete operations
                }
            }
        }
    )
}

suspend fun saveSelectedContacts(contactDao: ContactDao, contacts: List<Contact>) {
    withContext(Dispatchers.IO) {
        contacts.forEach { contact ->
            val contactEntity = ContactEntity(name = contact.name, phoneNumber = contact.phoneNumber)
            contactDao.insert(contactEntity)
        }
    }
}

// Function to update a contact in Room database
suspend fun updateContact(contactDao: ContactDao, contactEntity: ContactEntity) {
    withContext(Dispatchers.IO) {
        contactDao.update(contactEntity)
    }
}

// Function to delete a contact from Room database
suspend fun deleteContact(contactDao: ContactDao, contactEntity: ContactEntity) {
    withContext(Dispatchers.IO) {
        contactDao.delete(contactEntity)
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
                    AppNavHost(navController = rememberNavController(), context = this)
                }

            }
        }
    }
}


//@OptIn(ExperimentalMaterial3Api::class)
//@Preview(showBackground = false)
//@Composable
//fun GreetingPreview() {
//    ContactAppTheme {
//        Box(
//            modifier = Modifier.fillMaxSize().background(Color.White),
//        ) {
//            ContactAppPage()
//        }
//    }
//}