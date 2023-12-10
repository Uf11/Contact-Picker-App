package com.example.contactapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.example.contactapp.ui.theme.ContactAppTheme

class ContactMainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val contactViewModel = ContactViewModel(application)
        super.onCreate(savedInstanceState)
            setContent {
                ContactAppTheme {
                    ContactAppPage(contactViewModel = contactViewModel)
                }
            }
    }
}
@ExperimentalMaterial3Api
@Composable
fun ContactAppPage(contactViewModel: ContactViewModel) {
    val contextPkg = LocalContext.current
    val context = contextPkg.applicationContext
    val onCallClickFun: (String) -> Unit = { phoneNumber ->
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    val onMessageClickFun: (String) -> Unit = { phoneNumber ->
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("sms:$phoneNumber")
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    val onDeleteClickFun:(ContactEntity)->Unit = { contact ->
        contactViewModel.delete(contact)
    }
    val contactSentList by contactViewModel.allContacts.observeAsState(initial = emptyList())
    val addContactIntent = Intent(contextPkg, ImportActivity::class.java)
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
                    contextPkg.startActivity(addContactIntent)
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
            items(contactSentList) { contactEnt -> ContactCardMain(
                contact = contactEnt,
                onCallClick = { onCallClickFun(contactEnt.phoneNumber) },
                onMessageClick = { onMessageClickFun(contactEnt.phoneNumber) },
                onDeleteClick = { onDeleteClickFun(contactEnt) },
                contactViewModel = contactViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCardMain(
    contact: ContactEntity,
    onCallClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onDeleteClick: (ContactEntity) -> Unit,
    contactViewModel: ContactViewModel
) {

    val onEditClick:(ContactEntity)->Unit = { contact ->
        contactViewModel.update(contact)
    }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(contact.name) }
    var editedPhoneNumber by remember { mutableStateOf(contact.phoneNumber) }
    var selectedImageUri by remember { mutableStateOf(contact.imageUri?.toUri()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedImageUri = uri
    }
//    LaunchedEffect(selectedImageUri, editedName, editedPhoneNumber) {
//        onEditClick(ContactEntity(id = contact.id, name = editedName, phoneNumber = editedPhoneNumber, imageUri = selectedImageUri.toString()))
//    }
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
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Name and Number
            Row (
                modifier = Modifier
                    .fillMaxWidth()
//                    .padding(bottom = 8.dp)
            ){
                Column (
                    modifier = Modifier
                        .weight(1f)
                ){
                    if (isEditing) {
                        TextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text("Name") },
                            singleLine = true
                        )
                    } else {
                        Text(text = editedName)
                    }
                    if (isEditing) {
                        TextField(
                            value = editedPhoneNumber,
                            onValueChange = { editedPhoneNumber = it },
                            label = { Text("Phone Number") },
                            singleLine = true
                        )
                    } else {
                        Text(text = editedPhoneNumber)
                    }
                }
                Box(
                    modifier = Modifier
                        .clickable {
                            launcher.launch("image/*") ;
                            isEditing = true
                        }
                        .weight(1f),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "Contact Image",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Image",
                        modifier = Modifier
                            .size(15.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { onCallClick(contact.phoneNumber) }) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = "Call")
                }
                IconButton(onClick = { onMessageClick(contact.phoneNumber) }) {
                    Icon(imageVector = Icons.Default.MailOutline, contentDescription = "Message")
                }
                IconButton(onClick = {
                    if (isEditing) {
                        onEditClick(
                            ContactEntity(
                                id = contact.id,
                                name = editedName,
                                phoneNumber = editedPhoneNumber,
                                imageUri = selectedImageUri.toString()
                            )
                        )
                    }
                    isEditing = !isEditing
                }) {
                    Icon(imageVector = if (isEditing) Icons.Default.Done else Icons.Default.Edit, contentDescription = if (isEditing) "Save" else "Edit")
                }
                IconButton(onClick = { onDeleteClick(contact) }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

//@Preview
//@Composable
//fun ContactCardMainPreview() {
//    ContactAppTheme {
//        ContactCardMain(
//            contact = ContactEntity(id = 0, name = "John Doe", phoneNumber = "1234567890", imageUri = null),
//            onCallClick = {},
//            onMessageClick = {},
//            onEditClick = {},
//            onDeleteClick = {}
//        )
//    }
//}