document.addEventListener('DOMContentLoaded', () => {
    const dropZone = document.getElementById('dropZone');
    const fileInput = document.getElementById('fileInput');
    const submitBtn = document.getElementById('submitBtn');
    const uploadForm = document.getElementById('uploadForm');
    const filePreview = document.getElementById('filePreview');
    const fileNameSpan = document.getElementById('fileName');
    const removeFileBtn = document.getElementById('removeFileBtn');
    const statusMessage = document.getElementById('statusMessage');
    const passwordSection = document.getElementById('passwordSection');
    const passwordInput = document.getElementById('passwordInput');

    let selectedFile = null;

    // --- Lógica de la interfaz (sin cambios) ---
    dropZone.addEventListener('click', () => fileInput.click());
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('drop-zone--over'); });
    ['dragleave', 'dragend'].forEach(type => { dropZone.addEventListener(type, () => { dropZone.classList.remove('drop-zone--over'); }); });
    dropZone.addEventListener('drop', (e) => { e.preventDefault(); dropZone.classList.remove('drop-zone--over'); if (e.dataTransfer.files.length > 0) { updateFileDisplay(e.dataTransfer.files[0]); } });
    fileInput.addEventListener('change', () => { if (fileInput.files.length > 0) { updateFileDisplay(fileInput.files[0]); } });
    function updateFileDisplay(file) { selectedFile = file; fileNameSpan.textContent = file.name; filePreview.classList.remove('hidden'); filePreview.classList.add('flex'); passwordSection.classList.remove('hidden'); dropZone.classList.add('hidden'); submitBtn.disabled = false; statusMessage.textContent = ''; }
    removeFileBtn.addEventListener('click', () => { selectedFile = null; fileInput.value = ''; passwordInput.value = ''; filePreview.classList.add('hidden'); passwordSection.classList.add('hidden'); filePreview.classList.remove('flex'); dropZone.classList.remove('hidden'); submitBtn.disabled = true; });

    // --- LÓGICA DE ENVÍO AL BACKEND (ACTUALIZADA) ---
    uploadForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!selectedFile) return;

        const formData = new FormData();
        formData.append('file', selectedFile);

        if (passwordInput.value) {
            formData.append('password', passwordInput.value);
        }

        submitBtn.disabled = true;
        submitBtn.innerHTML = `
            <svg class="animate-spin h-5 w-5 mr-3 inline-block" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Procesando...
        `;
        statusMessage.textContent = 'Tu archivo está siendo analizado. Esto puede tardar unos segundos...';
        statusMessage.classList.remove('text-red-600', 'text-green-600');
        statusMessage.classList.add('text-blue-500');

        try {
            // =========== LÓGICA DE "CARRILES" ===========
            const statementType = document.querySelector('input[name="statement_type"]:checked').value;
            let uploadUrl = '/api/files/upload-account'; // URL por defecto para Cuentas
            let downloadFileName = `ConciliacionBancaria_${new Date().toISOString().slice(0, 10)}.xlsx`;

            if (statementType === 'credit_card') {
                uploadUrl = '/api/files/upload-credit-card'; // URL para Tarjetas de Crédito
                downloadFileName = `ReporteTarjetaCredito_${new Date().toISOString().slice(0, 10)}.xlsx`;
            }
            // ============================================

            const response = await axios.post(uploadUrl, formData, {
                responseType: 'blob',
            });

            const url = window.URL.createObjectURL(new Blob([response.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', downloadFileName); // Nombre de archivo dinámico
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            statusMessage.textContent = `¡Éxito! Tu archivo "${downloadFileName}" se ha descargado.`;
            statusMessage.classList.remove('text-blue-500');
            statusMessage.classList.add('text-green-600');

        } catch (error) {
            let errorMessage = '¡Error! No se pudo procesar el archivo. Por favor, inténtalo de nuevo.';
            if (error.response && error.response.status === 400) {
                 // Para leer el mensaje de error que viene como blob de texto
                const errorBlob = new Blob([error.response.data], { type: 'text/plain' });
                errorMessage = await errorBlob.text();
            }
            statusMessage.textContent = errorMessage;
            statusMessage.classList.remove('text-blue-500');
            statusMessage.classList.add('text-red-600');
            console.error('Error en la subida:', error);
        } finally {
            submitBtn.innerHTML = 'Convertir a Excel';
            removeFileBtn.click();
        }
    });
});