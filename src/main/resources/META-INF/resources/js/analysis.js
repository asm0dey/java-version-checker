/**
 * Analysis Page JavaScript - Stateless Frontend Implementation
 * Handles fake data generation, blur effect, form validation, and result reveal
 */

document.addEventListener('DOMContentLoaded', function() {
    const modalOverlay = document.getElementById('modal-overlay');
    const assessmentForm = document.getElementById('assessment-form');
    const submitBtn = document.getElementById('submit-btn');
    const loadingIndicator = document.getElementById('loading-indicator');
    const tablesContainer = document.getElementById('tables-container');

    // Form fields
    const emailInput = document.getElementById('email');
    const companyNameInput = document.getElementById('companyName');
    const roleInput = document.getElementById('role');

    // Error elements
    const emailError = document.getElementById('email-error');
    const companyNameError = document.getElementById('companyName-error');

    // Load analysis data from localStorage
    const analysisData = localStorage.getItem('analysisData');

    if (!analysisData) {
        // No data available, redirect to home
        window.location.href = '/';
        return;
    }

    let realResults;
    try {
        realResults = JSON.parse(analysisData);
        if (!Array.isArray(realResults) || realResults.length === 0) {
            throw new Error('Invalid data');
        }
    } catch (e) {
        console.error('Failed to parse analysis data:', e);
        window.location.href = '/';
        return;
    }

    // Generate fake data for preview
    const fakeData = generateFakeData(realResults);

    // Render both tables
    renderTables(fakeData, realResults);

    // Hide loading indicator
    loadingIndicator.classList.add('hidden');

    // Form submission handler
    assessmentForm.addEventListener('submit', async function(e) {
        e.preventDefault();

        // Clear previous errors
        emailError.classList.remove('visible');
        companyNameError.classList.remove('visible');

        // Validate form fields
        let isValid = true;

        // Email validation (FR-007)
        const emailValue = emailInput.value.trim();
        if (!emailValue || !isValidEmail(emailValue)) {
            emailError.classList.add('visible');
            isValid = false;
        }

        // Company name validation
        const companyNameValue = companyNameInput.value.trim();
        if (!companyNameValue) {
            companyNameError.classList.add('visible');
            companyNameError.textContent = 'Company name is required';
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Disable submit button (FR-017)
        submitBtn.disabled = true;
        submitBtn.textContent = 'Submitting...';

        try {
            // Send form data to backend (FR-016 - just for validation, no persistence)
            const formData = new URLSearchParams();
            formData.append('email', emailValue);
            formData.append('companyName', companyNameValue);
            if (roleInput.value.trim()) {
                formData.append('role', roleInput.value.trim());
            }

            const response = await fetch('/analysis/submit', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData.toString()
            });

            if (response.ok) {
                // Form validated, reveal results
                revealResults();
            } else {
                alert('Failed to submit form. Please try again.');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Submit & View Results';
            }
        } catch (error) {
            console.error('Error submitting form:', error);
            alert('An error occurred. Please try again.');
            submitBtn.disabled = false;
            submitBtn.textContent = 'Submit & View Results';
        }
    });

    /**
     * Generate fake preview data (FR-005)
     */
    function generateFakeData(realData) {
        return realData.map(() => ({
            javaVersion: 'XX.X.XX',
            javaRuntimeVersion: 'XX.X.XX-XXXX',
            javaVmVersion: 'XX.X.XX-XXXX',
            javaVendor: 'XXXX',
            javaVmVendor: 'XXXX',
            fileName: 'XXXX.properties',
            isOlderThanJdk8: false,
            requiresCommercialLicense: false,
            licenseExplanation: 'XXXXXXXXXXXXXXXXXXXX',
            versionAge: 'OK'
        }));
    }

    /**
     * Render both fake and real tables
     */
    function renderTables(fakeData, realData) {
        // Calculate statistics for real data
        const stats = calculateStatistics(realData);
        const fakeStats = {
            requiresPaidLicense: 'X',
            willRequireLicense: 'X',
            reachedEOL: 'X',
            totalVersions: 'XX'
        };

        tablesContainer.innerHTML = `
            <!-- Fake Preview (blurred) -->
            <div class="blurred" id="fake-preview-container">
                ${generateHeaderAndCards(fakeStats)}
                <div class="table-container">
                    ${generateTable(fakeData)}
                </div>
            </div>

            <!-- Real Results (hidden) -->
            <div class="hidden" id="real-results-container">
                ${generateHeaderAndCards(stats)}
                <div class="table-container">
                    ${generateTable(realData)}
                </div>
            </div>
        `;
    }

    /**
     * Calculate statistics from analysis data
     */
    function calculateStatistics(data) {
        const requiresPaidLicense = data.filter(v => v.requiresCommercialLicense).length;
        const reachedEOL = data.filter(v => v.isOlderThanJdk8).length;
        const totalVersions = data.length;

        return {
            requiresPaidLicense,
            willRequireLicense: requiresPaidLicense,
            reachedEOL,
            totalVersions
        };
    }

    /**
     * Generate header section and summary cards
     */
    function generateHeaderAndCards(stats) {
        // Determine recommendation level based on stats
        let recommendation = "Keep as is";
        let outcome = "You're Good!";

        if (stats.reachedEOL > 0) {
            recommendation = "Migration required";
            outcome = "You're at Risk!";
        } else if (stats.requiresPaidLicense > 0) {
            recommendation = "License required";
            outcome = "Action Needed!";
        }

        return `
            <div class="results-header">
                <div class="recommendation">Recommendation – ${recommendation}</div>
                <h1 class="outcome">${outcome}</h1>
                <p class="outcome-description">Welcome to our Java Version Analysis. Below you'll find a detailed assessment of your Java versions, licensing requirements, and recommendations for keeping your infrastructure secure and compliant.</p>
            </div>
            <div class="summary-cards">
                <div class="summary-card">
                    <div class="summary-number">${stats.requiresPaidLicense}/${stats.totalVersions}</div>
                    <div class="summary-label">Requires a Paid license. Migration required</div>
                </div>
                <div class="summary-card">
                    <div class="summary-number">${stats.willRequireLicense}/${stats.totalVersions}</div>
                    <div class="summary-label">Will require license from XXXX. Migration will require.</div>
                </div>
                <div class="summary-card">
                    <div class="summary-number">${stats.reachedEOL}/${stats.totalVersions}</div>
                    <div class="summary-label">reached EOL.</div>
                </div>
                <div class="summary-card">
                    <div class="summary-number">XXX</div>
                    <div class="summary-label">CVEs found in your workloads.</div>
                </div>
            </div>
        `;
    }

    /**
     * Generate HTML table matching results.png design
     */
    function generateTable(data) {
        const rows = data.map(version => {
            const licenseStatus = version.requiresCommercialLicense
                ? 'License required'
                : 'License not required';

            const eolDate = version.isOlderThanJdk8 ? 'End of Life' : 'See Oracle support';

            let recommendation;
            if (version.isOlderThanJdk8) {
                recommendation = 'Migration required';
            } else if (version.requiresCommercialLicense) {
                recommendation = 'License required';
            } else if (version.versionAge === 'OLD') {
                recommendation = 'Migration recommended';
            } else {
                recommendation = 'Keep as is';
            }

            return `
                <tr>
                    <td><strong>${escapeHtml(version.javaVersion || 'N/A')}</strong></td>
                    <td>${escapeHtml(version.licenseExplanation || 'N/A')}</td>
                    <td>${licenseStatus}</td>
                    <td>-</td>
                    <td>${eolDate}</td>
                    <td>${recommendation}</td>
                </tr>
            `;
        }).join('');

        return `
            <table>
                <thead>
                    <tr>
                        <th>Oracle JDK Update</th>
                        <th>License (tag + explanation + source)</th>
                        <th>License Status (Commercial Production)</th>
                        <th>CVE</th>
                        <th>End of Life (EOL)</th>
                        <th>Recommendation</th>
                    </tr>
                </thead>
                <tbody>
                    ${rows}
                </tbody>
            </table>
        `;
    }

    /**
     * Validates email format (FR-007)
     */
    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    /**
     * Reveals real results and hides fake preview (FR-009)
     */
    function revealResults() {
        // Hide modal overlay
        modalOverlay.classList.add('hidden');

        const fakeContainer = document.getElementById('fake-preview-container');
        const realContainer = document.getElementById('real-results-container');

        // Hide fake preview container
        fakeContainer.classList.add('hidden');

        // Show real results container
        realContainer.classList.remove('hidden');

        // Smooth scroll to results
        realContainer.scrollIntoView({ behavior: 'smooth', block: 'start' });

        // Clear localStorage after successful reveal
        localStorage.removeItem('analysisData');
    }

    /**
     * Escape HTML to prevent XSS
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Optional: Add real-time validation feedback
    emailInput.addEventListener('blur', function() {
        const emailValue = emailInput.value.trim();
        if (emailValue && !isValidEmail(emailValue)) {
            emailError.classList.add('visible');
        } else {
            emailError.classList.remove('visible');
        }
    });

    companyNameInput.addEventListener('blur', function() {
        const companyNameValue = companyNameInput.value.trim();
        if (!companyNameValue) {
            companyNameError.classList.add('visible');
            companyNameError.textContent = 'Company name is required';
        } else {
            companyNameError.classList.remove('visible');
        }
    });

    // Clear error on input
    emailInput.addEventListener('input', function() {
        if (emailError.classList.contains('visible')) {
            const emailValue = emailInput.value.trim();
            if (emailValue && isValidEmail(emailValue)) {
                emailError.classList.remove('visible');
            }
        }
    });

    companyNameInput.addEventListener('input', function() {
        if (companyNameError.classList.contains('visible') && companyNameInput.value.trim()) {
            companyNameError.classList.remove('visible');
        }
    });
});
