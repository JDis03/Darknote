package com.darknote.core.data

import com.darknote.core.model.Folder
import com.darknote.core.model.Snippet
import com.darknote.core.model.SyncStatus
import com.darknote.core.repository.FolderRepository
import com.darknote.core.repository.SnippetRepository
import com.darknote.core.storage.FileStorageService
import kotlinx.coroutines.flow.firstOrNull

/**
 * Initializes demo data if the database is empty.
 */
class DemoDataInitializer(
    private val folderRepository: FolderRepository,
    private val snippetRepository: SnippetRepository,
    private val fileStorageService: FileStorageService
) {
    
    suspend fun initializeIfEmpty() {
        // Check if database already has data
        val existingFolders = folderRepository.getAll().firstOrNull()
        if (!existingFolders.isNullOrEmpty()) {
            println("Database already has data, skipping initialization")
            return
        }
        
        println("Initializing demo data...")
        
        // Create folders
        val scriptsFolder = Folder(
            id = "folder-scripts",
            name = "Scripts",
            parentId = null,
            sortOrder = 0,
            createdAt = System.currentTimeMillis()
        )
        
        val databaseFolder = Folder(
            id = "folder-database",
            name = "Database",
            parentId = null,
            sortOrder = 1,
            createdAt = System.currentTimeMillis()
        )
        
        val serverConfigFolder = Folder(
            id = "folder-server-config",
            name = "Server Config",
            parentId = null,
            sortOrder = 2,
            createdAt = System.currentTimeMillis()
        )
        
        folderRepository.create(scriptsFolder)
        folderRepository.create(databaseFolder)
        folderRepository.create(serverConfigFolder)
        
        // Create snippets
        val snippets = listOf(
            Snippet(
                id = "snippet-backup-db",
                title = "backup-database.sh",
                content = """#!/bin/bash
# Backup database script
mysqldump -u root -p database > backup.sql
echo "Backup completed successfully!"
""",
                folderId = scriptsFolder.id,
                language = "bash",
                isFavorite = true,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                localPath = fileStorageService.generateSafePath("backup-database.sh"),
                syncStatus = SyncStatus.SYNCED
            ),
            Snippet(
                id = "snippet-deploy-server",
                title = "deploy-server.sh",
                content = """#!/bin/bash
# Deploy server script
echo 'Deploying to production...'
git pull origin main
docker-compose up -d --build
echo 'Deployment complete!'
""",
                folderId = scriptsFolder.id,
                language = "bash",
                isFavorite = false,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                localPath = fileStorageService.generateSafePath("deploy-server.sh"),
                syncStatus = SyncStatus.SYNCED
            ),
            Snippet(
                id = "snippet-nginx-restart",
                title = "nginx-restart.sh",
                content = """#!/bin/bash
# Restart nginx
sudo systemctl restart nginx
sudo systemctl status nginx
""",
                folderId = scriptsFolder.id,
                language = "bash",
                isFavorite = false,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                localPath = fileStorageService.generateSafePath("nginx-restart.sh"),
                syncStatus = SyncStatus.SYNCED
            ),
            Snippet(
                id = "snippet-mysql-optimize",
                title = "mysql-optimize.sql",
                content = """-- Optimize tables
OPTIMIZE TABLE users;
OPTIMIZE TABLE posts;
OPTIMIZE TABLE comments;

-- Analyze tables
ANALYZE TABLE users;
ANALYZE TABLE posts;
""",
                folderId = databaseFolder.id,
                language = "sql",
                isFavorite = false,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                localPath = fileStorageService.generateSafePath("mysql-optimize.sql"),
                syncStatus = SyncStatus.SYNCED
            ),
            Snippet(
                id = "snippet-create-user",
                title = "create-user.sql",
                content = """-- Create new database user
CREATE USER 'admin'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON database.* TO 'admin'@'localhost';
FLUSH PRIVILEGES;
""",
                folderId = databaseFolder.id,
                language = "sql",
                isFavorite = false,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                localPath = fileStorageService.generateSafePath("create-user.sql"),
                syncStatus = SyncStatus.SYNCED
            ),
            Snippet(
                id = "snippet-nginx-conf",
                title = "nginx.conf",
                content = """server {
    listen 80;
    server_name example.com;
    
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade ${"$"}http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host ${"$"}host;
        proxy_cache_bypass ${"$"}http_upgrade;
    }
}
""",
                folderId = serverConfigFolder.id,
                language = "config",
                isFavorite = true,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                localPath = fileStorageService.generateSafePath("nginx.conf"),
                syncStatus = SyncStatus.SYNCED
            ),
            Snippet(
                id = "snippet-quick-commands",
                title = "quick-commands.txt",
                content = """# Quick terminal commands
ls -la
cd ~
pwd
df -h
free -m
top
htop
""",
                folderId = null, // Root level
                language = "text",
                isFavorite = true,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                localPath = fileStorageService.generateSafePath("quick-commands.txt"),
                syncStatus = SyncStatus.SYNCED
            )
        )
        
        // Save snippets
        snippets.forEach { snippet ->
            snippetRepository.create(snippet)
            fileStorageService.saveSnippetContent(snippet)
        }
        
        println("Demo data initialized successfully!")
    }
}
