package com.google.firebase.functions

class TestFirebaseException(
    message: String,
    code: FirebaseFunctionsException.Code,
    details: Any? = null
) : FirebaseFunctionsException(message, code, details)
