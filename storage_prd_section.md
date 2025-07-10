# Product Requirements: Document Storage

## 1. Overview

This document outlines the technical requirements for the storage of scanned documents and their associated metadata within the SimplyScannerPro application. The primary goal is to ensure data integrity, security, and efficient access while adhering to the app's privacy-first, offline-capable model.

## 2. Storage Mechanism

### 2.1. File-System-Based Storage

The application will use a **directory-based storage model** on the user's local device. Each scanned document, including all its pages and metadata, will be encapsulated within its own dedicated folder. This approach avoids the need for a complex database, enhances data portability, and simplifies the backup and restore process.

### 2.2. Storage Location

All document folders will be stored within a designated subdirectory inside the application's standard `Documents` directory.

## 3. Data Model and Structure

### 3.1. The `ScanItem` Data Model

The core of the storage system is the `ScanItem` data model. This model will be a `Codable` Swift class, allowing for easy serialization to and from JSON. It will represent both a single scanned document and a folder containing other documents.

**Key Attributes of `ScanItem`:**

*   `uuid`: A unique identifier (UUID) for each item.
*   `displayName`: The user-visible name of the document or folder.
*   `bDir`: A boolean flag indicating if the item is a folder (`true`) or a document (`false`).
*   `order`: An ordered array of strings, where each string is the filename of a page image. This maintains the correct page sequence.
*   `bLock`: A boolean flag to indicate if the item is locked with a PIN.
*   `createdDate`, `updatedDate`, `deletedDate`: Timestamps to manage the item's lifecycle.

### 3.2. Directory Structure for a Single Document

Each document's folder will contain the following:

*   **`desc.json`**: A JSON file that is the serialized representation of the `ScanItem` object. This file stores all metadata for the document.
*   **Page Images**: Each page of the document will be stored as a separate image file (e.g., `page_1.jpg`, `page_2.jpg`). The filenames will correspond to the entries in the `order` array of the `desc.json` file.

## 4. Core Functional Requirements

### 4.1. Document Creation

When a new document is created, the system must:
1.  Create a new folder with a unique name on the file system.
2.  Create an instance of the `ScanItem` model.
3.  Save the initial page images to the new folder.
4.  Serialize the `ScanItem` object to `desc.json` and save it within the folder.

### 4.2. Document Modification

When a document is modified (e.g., pages are reordered, deleted, or added), the system must:
1.  Update the corresponding image files on disk.
2.  Update the `ScanItem` object in memory.
3.  Re-serialize the updated `ScanItem` object and overwrite the existing `desc.json` file.

### 4.3. Document Retrieval

1.  To list all documents, the system will scan the main storage directory and read the `desc.json` file from each subfolder.
2.  To open a single document, the system will read its `desc.json` file to get the metadata and then load the page images based on the `order` array.

## 5. Non-Functional Requirements

### 5.1. Data Integrity

The system must ensure that the `desc.json` file is always consistent with the image files stored in the directory. All file operations should be atomic where possible.

### 5.2. Offline Capability

The entire storage mechanism must be fully functional without an internet connection.

### 5.3. Performance

Reading and writing to the file system should be performed efficiently to ensure a responsive user experience, especially when dealing with large documents or a large number of documents.

## 6. The `ScanService` Abstraction Layer

All file system interactions will be managed by a dedicated `ScanService` layer. This service will provide a clean and high-level API for the rest of the application (e.g., `createDocument()`, `updateDocument()`, `deleteDocument()`) and will encapsulate all the low-level details of file and directory manipulation.