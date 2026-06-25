# NexCart API Design

## Base URL

```text
/api/v1
```

> **Note:** The OpenAPI (Swagger) specification is the source of truth for API details. This document provides a high-level overview of the API design.

---

## User Service

### Create User

**Endpoint**

```http
POST /api/v1/users
```

**Request**

```json
{
  "firstName": "Nithya",
  "lastName": "Mukundan",
  "email": "nithya@example.com",
  "phoneNumber": "9876543210"
}
```

**Success Response**

`201 Created`

```json
{
  "id": 1,
  "firstName": "Nithya",
  "lastName": "Mukundan",
  "email": "nithya@example.com",
  "phoneNumber": "9876543210"
}
```

**Possible Error Responses**

| Status | Description          |
| -----: | -------------------- |
|    400 | Validation failed    |
|    409 | Email already exists |

---

### Get User

**Endpoint**

```http
GET /api/v1/users/{id}
```

**Success Response**

`200 OK`

**Possible Error Responses**

| Status | Description    |
| -----: | -------------- |
|    404 | User not found |

---

### Get All Users

**Endpoint**

```http
GET /api/v1/users
```

**Success Response**

`200 OK`

---

### Update User

**Endpoint**

```http
PUT /api/v1/users/{id}
```

**Success Response**

`200 OK`

**Possible Error Responses**

| Status | Description          |
| -----: | -------------------- |
|    400 | Validation failed    |
|    404 | User not found       |
|    409 | Email already exists |

---

### Delete User

**Endpoint**

```http
DELETE /api/v1/users/{id}
```

**Success Response**

`204 No Content`

**Possible Error Responses**

| Status | Description    |
| -----: | -------------- |
|    404 | User not found |
