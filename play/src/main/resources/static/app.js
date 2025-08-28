document.addEventListener('DOMContentLoaded', () => {

    /**
     * Función reutilizable para inicializar una tarjeta de subida de archivos.
     * @param {string} formClass - La clase CSS del formulario de la tarjeta.
     * @param {string} uploadUrl - El endpoint del backend al que se enviará el archivo.
     * @param {string} downloadFileNameBase - El nombre base para el archivo descargado.
     */
    function initializeUploader(formClass, uploadUrl, downloadFileNameBase) {
        const uploadCard = document.querySelector(`.${formClass}`).closest('.upload-card');
        if (!uploadCard) return;

        const uploadForm = uploadCard.querySelector('form');
        const dropZone = uploadCard.querySelector('.drop-zone');
        const fileInput = uploadCard.querySelector('.file-input');
        const submitBtn = uploadCard.querySelector('.submit-btn');
        const filePreview = uploadCard.querySelector('.file-preview');
        const fileNameSpan = uploadCard.querySelector('.file-name');
        const removeFileBtn = uploadCard.querySelector('.remove-file-btn');
        const statusMessage = uploadCard.querySelector('.status-message');
        const passwordSection = uploadCard.querySelector('.password-section');
        const passwordInput = uploadCard.querySelector('.password-input');
        
        let selectedFile = null;

        // --- Lógica de la interfaz ---
        dropZone.addEventListener('click', () => fileInput.click());
        dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('drop-zone--over'); });
        ['dragleave', 'dragend'].forEach(type => { dropZone.addEventListener(type, () => { dropZone.classList.remove('drop-zone--over'); }); });
        dropZone.addEventListener('drop', (e) => { e.preventDefault(); dropZone.classList.remove('drop-zone--over'); if (e.dataTransfer.files.length > 0) { updateFileDisplay(e.dataTransfer.files[0]); } });
        fileInput.addEventListener('change', () => { if (fileInput.files.length > 0) { updateFileDisplay(fileInput.files[0]); } });
        
        function updateFileDisplay(file) {
            selectedFile = file;
            fileNameSpan.textContent = file.name;
            filePreview.classList.remove('hidden');
            passwordSection.classList.remove('hidden');
            dropZone.style.display = 'none';
            submitBtn.disabled = false;
            statusMessage.textContent = '';
        }

        removeFileBtn.addEventListener('click', () => {
            selectedFile = null;
            fileInput.value = '';
            passwordInput.value = '';
            filePreview.classList.add('hidden');
            passwordSection.classList.add('hidden');
            dropZone.style.display = 'block';
            submitBtn.disabled = true;
        });

        // --- Lógica de envío al backend ---
        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!selectedFile) return;

            const formData = new FormData();
            formData.append('file', selectedFile);
            if (passwordInput.value) {
                formData.append('password', passwordInput.value);
            }

            submitBtn.disabled = true;
            submitBtn.innerHTML = `Procesando...`;
            statusMessage.textContent = 'Tu archivo está siendo analizado...';
            statusMessage.className = 'status-message mt-4 text-xs text-blue-500';

            try {
                const response = await axios.post(uploadUrl, formData, { responseType: 'blob' });

                const url = window.URL.createObjectURL(new Blob([response.data]));
                const link = document.createElement('a');
                link.href = url;
                const fileName = `${downloadFileNameBase}_${new Date().toISOString().slice(0, 10)}.xlsx`;
                link.setAttribute('download', fileName);
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.URL.revokeObjectURL(url);

                statusMessage.textContent = `¡Éxito! Tu archivo se ha descargado.`;
                statusMessage.className = 'status-message mt-4 text-xs text-green-600';

            } catch (error) {
                let errorMessage = 'Error: No se pudo procesar el archivo.';
                if (error.response && error.response.data) {
                    const errorBlob = new Blob([error.response.data], { type: 'text/plain' });
                    errorMessage = await errorBlob.text();
                }
                statusMessage.textContent = errorMessage;
                statusMessage.className = 'status-message mt-4 text-xs text-red-600';
            } finally {
                submitBtn.innerHTML = 'Convertir';
                removeFileBtn.click();
            }
        });
    }

    // =================================================================================
    // AQUÍ INICIALIZAMOS CADA TARJETA CON SU CONFIGURACIÓN ESPECÍFICA
    // =================================================================================
    
    initializeUploader(
        'bancolombia-account-form',
        '/api/bancolombia-account/process',
        'Conciliacion_Bancolombia_Cuentas'
    );

    initializeUploader(
        'davivienda-account-form',
        '/api/davivienda-account/process',
        'Conciliacion_Davivienda_Cuentas'
    );

    // Activamos la nueva tarjeta para Tarjetas de Crédito de Bancolombia
    initializeUploader(
        'bancolombia-credit-card-form',
        '/api/bancolombia-credit-card/process',
        'Reporte_TC_Bancolombia'
    );
    
});