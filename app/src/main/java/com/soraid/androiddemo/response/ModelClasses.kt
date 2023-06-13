package com.soraid.androiddemo.response

data class VerificationSession(
    val id: String,
    val objectName: String,
    val activatedAuthenticationMethods: List<String>,
    val authenticated: Boolean,
    val authenticationMethods: List<String>,
    val checks: List<Check>,
    val completedAt: Long,
    val createdAt: Long,
    val customFields: Map<String, Any>,
    val email: String,
    val expiresAt: Long,
    val fieldsToCollect: List<String>,
    val ip: List<String>,
    val phone: String?,
    val projectId: String,
    val redirectUrl: String?,
    val reportId: String?,
    val status: String,
    val token: String,
    val traits: Traits
)

data class Check(
    val name: String,
    val value: Boolean,
    val status: String
)

data class Traits(
    val address: Address,
    val email: String,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val secondFamilyName: String?,
    val fullLastName: String,
    val phone: String?,
    val ssn4: String?,
    val ssn9: String?,
    val identificationNumber: String?,
    val identificationType: String?,
) {
    override fun toString(): String {
        return "\n" +
                "Address: $address \n" +
                "Email: $email \n" +
                "FirstName: $firstName \n" +
                "LastName: $lastName \n" +
                "SecondFamilyName: $secondFamilyName \n" +
                "FullLastName: $fullLastName \n" +
                "Phone: $phone \n" +
                "Identification Number: $identificationNumber \n" +
                "Identification Type: $identificationType"
    }
}

data class Address(
    val line1: String,
    val line2: String?,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String
)

data class DateOfBirth(
    val day: Int,
    val month: Int,
    val year: Int
)

data class Document(
    val documentType: String,
    val issuingCountry: String,
    val documentNumber: String,
    val dateOfExpiry: DateOfBirth,
    val nationality: String?,
    val gender: String?,
    val dateOfBirth: DateOfBirth,
    val firstName: String,
    val lastName: String,
    val middleName: String
)
