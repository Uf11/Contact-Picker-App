package com.example.contactapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.contactapp.ui.theme.ContactAppTheme

class ImportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val contactViewModel = ContactViewModel(application)
        super.onCreate(savedInstanceState)
        setContent {
            ContactAppTheme {
                ImportContactsActivity(contactViewModel = contactViewModel, goBack= { finish() })
            }
        }
    }
}

@SuppressLint("Range")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsActivity(contactViewModel: ContactViewModel, goBack: () -> Unit) {
    val contentResolver = LocalContext.current.contentResolver

    val cursor =
        contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC")
    val tempContacts = emptyList<ContactEntity>().toMutableList()
    if (cursor != null && cursor.count > 0) {
        while (cursor.moveToNext()) {
            val hasNumber =
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
            val contactId =
                cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID))
            val name =
                cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            var phoneNumber = ""
            if (hasNumber > 0.toString()) {
                val cp = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(contactId.toString()),
                    null
                )
                if (cp != null && cp.moveToFirst()) {
                    phoneNumber =
                        cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    cp.close()
                }
            }
            tempContacts += ContactEntity(contactId, name, phoneNumber)
        }
        cursor.close()
    }
    var selectedList by remember { mutableStateOf(emptyList<ContactEntity>()) }
    var isSelectAllChecked by remember { mutableStateOf(false) }
    val onContactCheckedChange: (ContactEntity, Boolean) -> Unit = { contactEnt, isChecked ->
        selectedList = if (isChecked) {
            selectedList + contactEnt
        } else {
            selectedList - contactEnt
        }
        isSelectAllChecked = selectedList.size == tempContacts.size
    }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(text = "Import Contacts", textAlign = TextAlign.Center , modifier = Modifier.padding(end = 30.dp,top = 7.dp), style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.primary))

                Text(text = "Select All", textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.primary)
                Checkbox(
                    checked = isSelectAllChecked,
                    onCheckedChange = { isChecked ->
                        isSelectAllChecked = isChecked
                        selectedList = if (isChecked) {
                            tempContacts.toList()
                        } else {
                            emptyList()
                        }
                    },
                    modifier = Modifier
                        .padding(end = 8.dp)
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        goBack()
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(text = "Cancel")
                }
                Button(
                    onClick = {
                        selectedList.forEach { contactEntity ->
                            contactViewModel.insert(contactEntity)
                        }
                        goBack()
                    },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(text = "Import")
                }
            }
        },
        content = { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
            ) {
                items(tempContacts) { contact ->
                    val isChecked = selectedList.contains(contact)
                    ContactCardWithCheck(
                        contactEnt = contact,
                        isChecked = isChecked,
                        onCheckedChange = { isChecked -> onContactCheckedChange(contact, isChecked) }
                    )
                }
            }
        }
    )
}

@Composable
fun ContactCardWithCheck(contactEnt: ContactEntity, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
            Text(text = contactEnt.name)
            Text(text = contactEnt.phoneNumber)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckedChange(it) }
                )
            }
        }
    }
}