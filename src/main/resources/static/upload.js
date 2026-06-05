document.addEventListener('DOMContentLoaded', () => {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');
    const selectFileBtn = document.getElementById('selectFileBtn');
    const fileInfo = document.getElementById('fileInfo');
    const fileName = document.getElementById('fileName');
    const previewSection = document.getElementById('previewSection');
    const previewFilename = document.getElementById('previewFilename');
    const previewContent = document.getElementById('previewContent');
    const errorSection = document.getElementById('errorSection');
    const errorMessage = document.getElementById('errorMessage');
    const closeErrorBtn = document.getElementById('closeErrorBtn');
    const copyBtn = document.getElementById('copyBtn');
    const downloadBtn = document.getElementById('downloadBtn');
    const thumbsUpBtn = document.getElementById('thumbsUpBtn');
    const thumbsDownBtn = document.getElementById('thumbsDownBtn');

    let currentFilename = '';
    let currentContent = '';

    // File selection
    selectFileBtn.addEventListener('click', () => {
        fileInput.click();
    });

    fileInput.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFile(e.target.files[0]);
        }
    });

    // Drag and drop
    uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('dragover');
    });

    uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('dragover');
    });

    uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('dragover');

        const files = e.dataTransfer.files;
        if (files.length > 0) {
            handleFile(files[0]);
        }
    });

    // Handle file
    async function handleFile(file) {
        if (!file.name.endsWith('.kt')) {
            showError('Please select a Kotlin (.kt) file');
            return;
        }

        fileName.textContent = file.name;
        fileInfo.style.display = 'block';

        try {
            const content = await file.text();
            await uploadFile(content);
        } catch (error) {
            showError('Error reading file: ' + error.message);
        }
    }

    // Upload file
    async function uploadFile(content) {
        try {
            const response = await fetch('/api/upload', {
                method: 'POST',
                headers: {
                    'Content-Type': 'text/plain'
                },
                body: content
            });

            const data = await response.json();

            if (data.success) {
                currentFilename = data.data.filename;
                currentContent = data.data.content;
                showPreview();
                loadStats();
            } else {
                showError(data.error || 'Failed to generate test');
            }
        } catch (error) {
            showError('Error uploading file: ' + error.message);
        }
    }

    // Show preview
    function showPreview() {
        previewFilename.textContent = currentFilename;
        previewContent.textContent = currentContent;
        previewSection.style.display = 'block';
        errorSection.style.display = 'none';
        window.scrollTo({ top: previewSection.offsetTop, behavior: 'smooth' });
    }

    // Show error
    function showError(message) {
        errorMessage.textContent = message;
        errorSection.style.display = 'block';
        previewSection.style.display = 'none';
    }

    // Copy to clipboard
    copyBtn.addEventListener('click', () => {
        navigator.clipboard.writeText(currentContent).then(() => {
            const originalText = copyBtn.textContent;
            copyBtn.textContent = '✓ Copied!';
            setTimeout(() => {
                copyBtn.textContent = originalText;
            }, 2000);
        });
    });

    // Download
    downloadBtn.addEventListener('click', () => {
        const blob = new Blob([currentContent], { type: 'text/plain' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = currentFilename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    });

    // Feedback
    async function sendFeedback(useful) {
        try {
            await fetch('/api/feedback', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    filename: currentFilename,
                    useful: useful,
                    comment: ''
                })
            });

            const originalText = useful ? thumbsUpBtn.textContent : thumbsDownBtn.textContent;
            const btn = useful ? thumbsUpBtn : thumbsDownBtn;
            btn.textContent = '✓ Recorded';
            setTimeout(() => {
                btn.textContent = originalText;
            }, 2000);

            loadStats();
        } catch (error) {
            console.error('Error sending feedback:', error);
        }
    }

    thumbsUpBtn.addEventListener('click', () => {
        sendFeedback(true);
    });

    thumbsDownBtn.addEventListener('click', () => {
        sendFeedback(false);
    });

    // Close error
    closeErrorBtn.addEventListener('click', () => {
        errorSection.style.display = 'none';
    });

    // Load stats
    async function loadStats() {
        try {
            const response = await fetch('/api/stats');
            const stats = await response.json();

            if (stats.success) {
                document.getElementById('totalGenerated').textContent = stats.data.totalGenerated || '0';
                document.getElementById('usefulCount').textContent = stats.data.usefulCount || '0';
            }
        } catch (error) {
            console.error('Error loading stats:', error);
        }
    }

    // Load initial stats
    loadStats();
});
