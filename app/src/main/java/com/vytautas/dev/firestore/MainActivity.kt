package com.vytautas.dev.firestore

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.vytautas.dev.firestore.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    private val userCollectionRef = Firebase.firestore.collection("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUploadData.setOnClickListener {
            val user = getOldUser()
            saveUser(user)
            hideKeyboard(this)
        }

//        subscribeToRealtimeUpdates()

        binding.btnRetrieveData.setOnClickListener {
            retrieveUsers()
            hideKeyboard(this)
        }

        binding.btnUpdateUser.setOnClickListener {
            val oldUser = getOldUser()
            val newUser = getNewUser()
            updateUser(oldUser, newUser)
            hideKeyboard(this)
        }

        binding.btnDeleteData.setOnClickListener {
            val user = getOldUser()
            deleteUser(user)
        }

        binding.btnBatchedWrite.setOnClickListener {
            changeName("HT0ctoo0UxnGb75JulcW", "Kristina", "Kukaite")
        }

        binding.btnTransaction.setOnClickListener {
            incrementAge("HT0ctoo0UxnGb75JulcW")
        }
    }

    private fun getOldUser(): User {
        val firstName = binding.etFirstName.text.toString()
        val lastName = binding.etLastName.text.toString()
        val age = binding.etAge.text.toString().toInt()

        return User(firstName, lastName, age)
    }

    private fun getNewUser(): Map<String, Any> {
        val firstName = binding.etNewFirstName.text.toString()
        val lastName = binding.etNewLastName.text.toString()
        val age = binding.etNewAge.text.toString()

        val userMap = mutableMapOf<String, Any>()

        if (firstName.isNotEmpty()) {
            userMap["firstName"] = firstName
        }
        if (lastName.isNotEmpty()) {
            userMap["lastName"] = lastName
        }
        if (age.isNotEmpty()) {
            userMap["age"] = age.toInt()
        }

        return userMap
    }

    private fun deleteUser(user: User) =
        CoroutineScope(Dispatchers.IO).launch {
            val userQuery = userCollectionRef
                .whereEqualTo("firstName", user.firstName)
                .whereEqualTo("lastName", user.lastName)
                .whereEqualTo("age", user.age)
                .get()
                .await()
            if (userQuery.documents.isNotEmpty()) {
                for (document in userQuery) {
                    try {
                        userCollectionRef.document(document.id).delete().await()
                        /* userCollectionRef.document(document.id).update(
                             mapOf(
                                 "firstName" to FieldValue.delete()
                             )
                         )*/
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                e.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No person matched the query.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


    private fun updateUser(user: User, newUserMap: Map<String, Any>) =
        CoroutineScope(Dispatchers.IO).launch {
            val userQuery = userCollectionRef
                .whereEqualTo("firstName", user.firstName)
                .whereEqualTo("lastName", user.lastName)
                .whereEqualTo("age", user.age)
                .get()
                .await()
            if (userQuery.documents.isNotEmpty()) {
                for (document in userQuery) {
                    try {
                        userCollectionRef.document(document.id).set(
                            newUserMap,
                            SetOptions.merge()
                        ).await()
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                e.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@MainActivity,
                        "No person matched the query.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


    private fun incrementAge(
        userId: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runTransaction { transaction ->
                val userRef = userCollectionRef.document(userId)
                val user = transaction.get(userRef)
                val newAge = user["age"] as Long + 1
                transaction.update(userRef, "age", newAge)
                null
            }.await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun changeName(
        userId: String,
        newFirstName: String,
        newLastName: String
    ) = CoroutineScope(Dispatchers.IO).launch {
        try {
            Firebase.firestore.runBatch { batch ->
                val userRef = userCollectionRef.document(userId)
                batch.update(userRef, "firstName", newFirstName)
                batch.update(userRef, "lastName", newLastName)
            }.await()
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun subscribeToRealtimeUpdates() {
        userCollectionRef.addSnapshotListener { value, error ->
            error?.let {
                Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                return@addSnapshotListener
            }
            value?.let {
                val sb = StringBuilder()
                for (document in it) {
                    val user = document.toObject<User>()
                    sb.append("$user\n")
                }
                binding.tvPersons.text = sb.toString()
            }
        }
    }

    private fun retrieveUsers() = CoroutineScope(Dispatchers.IO).launch {

        val fromAge = binding.etFrom.text.toString().toInt()
        val toAge = binding.etTo.text.toString().toInt()


        try {
            val querySnapshot = userCollectionRef
                .whereGreaterThan("age", fromAge)
                .whereLessThan("age", toAge)
                .orderBy("age")
                .get()
                .await()
            val sb = StringBuilder()
            for (document in querySnapshot.documents) {
                val user = document.toObject<User>()
                sb.append("$user\n")
            }
            withContext(Dispatchers.Main) {
                binding.tvPersons.text = sb.toString()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveUser(user: User) = CoroutineScope(Dispatchers.IO).launch {
        try {
            userCollectionRef.add(user).await()
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Successfully saved data.", Toast.LENGTH_LONG)
                    .show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}