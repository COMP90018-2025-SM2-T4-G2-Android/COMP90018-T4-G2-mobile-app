# Firebase & Database Interaction Guide

## Quick Overview

CashPal uses **Firebase** for all backend services:
- **Firebase Authentication** - User login/signup
- **Firestore Database** - Store user data, transactions, contacts
- **Firebase Storage** - Store images, receipts, QR codes

## How to Use

### 1. Get the Repository

```kotlin
// Always get data through the CashPalRepository
val repository = ServiceLocator.getRepository()
```

### 2. User Authentication

```kotlin
// Sign up new user
repository.signUpWithEmail(email, password, displayName).collect { result ->
    result.fold(
        onSuccess = { /* User created successfully */ },
        onFailure = { error -> /* Handle error */ }
    )
}

// Sign in existing user
repository.signInWithEmail(email, password).collect { result ->
    // Handle result
}

// Google Sign-In
repository.signInWithGoogle(activity) { intent ->
    // Launch Google Sign-In intent
}
```

### 3. User Data

```kotlin
// Get current user profile
val userId = repository.getCurrentUserId()
repository.getUserProfile(userId).collect { result ->
    result.fold(
        onSuccess = { user -> 
            // Use user data: user.balance, user.email, etc.
        },
        onFailure = { /* Handle error */ }
    )
}

// Update user profile
repository.updateUserProfile(user).collect { result ->
    // Handle result
}
```

### 4. Transactions

```kotlin
// Get user transactions
repository.getUserTransactions(userId, limit = 10).collect { result ->
    result.fold(
        onSuccess = { transactions ->
            // Display transactions list
        },
        onFailure = { /* Handle error */ }
    )
}

// Create new transaction (sends money)
val transaction = Transaction(
    fromUserId = currentUserId,
    toUserId = recipientId,
    amount = 50.0,
    currency = "AUD",
    description = "Payment for lunch"
)

repository.createTransaction(transaction).collect { result ->
    // Transaction created and balances updated automatically
}
```

### 5. Contacts

```kotlin
// Get user contacts
repository.getUserContacts(userId).collect { result ->
    result.fold(
        onSuccess = { contacts ->
            // Display contacts list
        },
        onFailure = { /* Handle error */ }
    )
}

// Add new contact
val contact = Contact(
    userId = currentUserId,
    contactUserId = contactId,
    name = "John Doe",
    email = "john@example.com"
)

repository.createContact(contact).collect { result ->
    // Contact added
}
```

### 6. Payment Requests

```kotlin
// Get payment requests
repository.getPaymentRequests(userId).collect { result ->
    result.fold(
        onSuccess = { requests ->
            // Display payment requests
        },
        onFailure = { /* Handle error */ }
    )
}

// Accept payment request
repository.acceptPaymentRequest(requestId).collect { result ->
    // Payment processed and balances updated
}
```

## Important Notes

### ✅ Do This
- Always use `ServiceLocator.getRepository()` to get the repository
- Use `.collect { }` to handle Flow results
- Check `result.fold()` for success/failure
- Handle errors gracefully with user-friendly messages

### ❌ Don't Do This
- Don't create Firebase services directly
- Don't forget to handle errors
- Don't block the UI thread (use coroutines)
- Don't assume operations will always succeed

## Error Handling

```kotlin
repository.someOperation().collect { result ->
    result.fold(
        onSuccess = { data ->
            // Success - use the data
            updateUI(data)
        },
        onFailure = { error ->
            // Error - show user-friendly message
            Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    )
}
```

## Data Models

Key data classes you'll work with:

- **User** - User profile and balance
- **Transaction** - Money transfers
- **Contact** - User's contacts
- **PaymentRequest** - Payment requests from others

All models have default values, so you only need to set the fields you care about.

## Demo Mode

```kotlin
// Check if user is in demo mode
val isDemoMode = BiometricPreferences(context).isDemoMode()

if (isDemoMode) {
    // Show sample data
} else {
    // Load real Firebase data
}
```

## Quick Examples

### Send Money
```kotlin
val transaction = Transaction(
    fromUserId = currentUserId,
    toUserId = recipientId,
    amount = 25.0,
    currency = "AUD",
    description = "Coffee money"
)

repository.createTransaction(transaction).collect { result ->
    result.fold(
        onSuccess = { 
            Toast.makeText(this, "Payment sent!", Toast.LENGTH_SHORT).show()
        },
        onFailure = { error ->
            Toast.makeText(this, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    )
}
```

### Get Balance
```kotlin
repository.getUserProfile(userId).collect { result ->
    result.fold(
        onSuccess = { user ->
            val balance = user.balance
            val currency = user.currency
            balanceTextView.text = "$${String.format("%.2f", balance)} $currency"
        },
        onFailure = { error ->
            balanceTextView.text = "Error loading balance"
        }
    )
}
```

---

**That's it!** This covers 90% of what you need to interact with Firebase and the database in CashPal.
