package com.soraid.androiddemo.response

import org.json.JSONObject

fun parseTraits(response: JSONObject): Traits {
    val traitsJson = response.getJSONObject("traits")
    val addressJson = traitsJson.getJSONObject("address")
    val address = Address(
        line1 = addressJson.getString("line1"),
        line2 = addressJson.optString("line2", null),
        city = addressJson.getString("city"),
        state = addressJson.getString("state"),
        postalCode = addressJson.getString("postal_code"),
        country = addressJson.getString("country")
    )

    return Traits(
        address = address,
        email = traitsJson.getString("email"),
        firstName = traitsJson.getString("first_name"),
        lastName = traitsJson.getString("last_name"),
        middleName = traitsJson.optString("middle_name", null),
        secondFamilyName = traitsJson.optString("second_family_name", null),
        fullLastName = traitsJson.getString("full_last_name"),
        phone = traitsJson.optString("phone", null),
        ssn4 = traitsJson.optString("ssn4", null),
        ssn9 = traitsJson.optString("ssn9", null),
        identificationNumber = traitsJson.optString("identification_number", null),
        identificationType = traitsJson.optString("identification_type", null),
    )
}

private fun parseDocument(json: JSONObject): Document {
    val dateOfBirthJson = json.getJSONObject("date_of_birth")
    val dateOfBirth = DateOfBirth(
        day = dateOfBirthJson.getInt("day"),
        month = dateOfBirthJson.getInt("month"),
        year = dateOfBirthJson.getInt("year")
    )

    val dateOfExpiryJson = json.getJSONObject("date_of_expiry")
    val dateOfExpiry = DateOfBirth(
        day = dateOfExpiryJson.getInt("day"),
        month = dateOfExpiryJson.getInt("month"),
        year = dateOfExpiryJson.getInt("year")
    )

    return Document(
        documentType = json.getString("document_type"),
        issuingCountry = json.getString("issuing_country"),
        documentNumber = json.getString("document_number"),
        dateOfExpiry = dateOfExpiry,
        nationality = json.optString("nationality", null),
        gender = json.optString("gender", null),
        dateOfBirth = dateOfBirth,
        firstName = json.getString("first_name"),
        lastName = json.getString("last_name"),
        middleName = json.getString("middle_name")
    )
}
