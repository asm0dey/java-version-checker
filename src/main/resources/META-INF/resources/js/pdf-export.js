/**
 * PDF Export Module for Java Version Checker
 * Generates professional PDF reports from analysis results
 */

(function() {
    'use strict';

    // PDF Export configuration
    const CONFIG = {
        // PDF styling constants
        colors: {
            primary: [102, 126, 234],      // Indigo for headers
            secondary: [118, 75, 162],     // Purple accent
            textDark: [33, 33, 33],        // Dark text for light theme
            textLight: [255, 255, 255],    // White text
            success: [34, 197, 94],        // Green for OK
            warning: [234, 179, 8],        // Yellow for Old
            danger: [239, 68, 68],         // Red for Very Old
            border: [229, 231, 235],       // Light gray borders
            background: [249, 250, 251]    // Light background
        },
        fonts: {
            header: 16,
            subheader: 12,
            body: 10,
            small: 8
        },
        margins: {
            top: 15,
            right: 15,
            bottom: 20,
            left: 15
        }
    };

    /**
     * Extract analysis data from localStorage or page content
     * @returns {Array|null} Analysis data array or null if not found
     */
    function extractAnalysisData() {

        // First try localStorage
        try {
            const data = localStorage.getItem('analysisData');
            if (data) {
                const parsed = JSON.parse(data);
                if (Array.isArray(parsed) && parsed.length > 0) {
                    return parsed;
                }
            }
        } catch (error) {
            console.warn('[PDF Export] Failed to parse localStorage data:', error);
        }
        
        // Fallback: Extract from the table on the results page
        const tableData = extractFromTable();
        if (tableData && tableData.length > 0) {
            return tableData;
        }
        
        console.warn('[PDF Export] No analysis data found');
        return null;
    }

    /**
     * Extract data from the results table on the page
     * @returns {Array|null} Array of version objects or null
     */
    function extractFromTable() {

        const table = document.querySelector('table tbody');
        if (!table) {
            return null;
        }
        
        const rows = table.querySelectorAll('tr');
        if (rows.length === 0) {
            return null;
        }
        

        const data = [];
        rows.forEach((row, index) => {
            const cells = row.querySelectorAll('td');
            if (cells.length >= 7) {
                // Extract version badge text
                const versionBadge = cells[0].querySelector('.version-badge');
                const javaVersion = versionBadge ? versionBadge.textContent.trim() : cells[0].textContent.trim();
                
                // Extract runtime version
                const javaRuntimeVersion = cells[1].textContent.trim();
                
                // Extract VM version
                const javaVmVersion = cells[2].textContent.trim();
                
                // Extract vendor
                const vendorTag = cells[3].querySelector('.vendor-tag');
                const javaVendor = vendorTag ? vendorTag.textContent.trim() : cells[3].textContent.trim();
                
                // Extract VM vendor
                const javaVmVendor = cells[4].textContent.trim();
                
                // Extract version age from badge class
                const ageCell = cells[5];
                const ageBadge = ageCell.querySelector('.very-old-badge, .old-badge, .ok-badge');
                let versionAge = 'OK';
                if (ageBadge) {
                    if (ageBadge.classList.contains('very-old-badge')) {
                        versionAge = 'VERY_OLD';
                    } else if (ageBadge.classList.contains('old-badge')) {
                        versionAge = 'OLD';
                    } else if (ageBadge.classList.contains('ok-badge')) {
                        versionAge = 'OK';
                    }
                }
                
                // Extract license info
                const licenseCell = cells[6];
                const warningBadge = licenseCell.querySelector('.warning-badge');
                const successBadge = licenseCell.querySelector('.success-badge');
                const requiresCommercialLicense = !!warningBadge;
                const licenseTooltip = licenseCell.querySelector('.license-tooltip');
                const licenseExplanation = licenseTooltip ? licenseTooltip.textContent.trim() : 
                                          (requiresCommercialLicense ? 'Commercial license required' : 'Free / Open Source');
                
                data.push({
                    javaVersion: javaVersion || 'N/A',
                    javaRuntimeVersion: javaRuntimeVersion || 'N/A',
                    javaVmVersion: javaVmVersion || 'N/A',
                    javaVendor: javaVendor || 'N/A',
                    javaVmVendor: javaVmVendor || 'N/A',
                    fileName: 'extracted-from-table.properties',
                    isOlderThanJdk8: versionAge === 'VERY_OLD',
                    requiresCommercialLicense: requiresCommercialLicense,
                    licenseExplanation: licenseExplanation,
                    versionAge: versionAge
                });
            }
        });
        
        return data.length > 0 ? data : null;
    }

    /**
     * Calculate summary statistics from analysis data
     * @param {Array} data - Analysis data array
     * @returns {Object} Summary statistics
     */
    function calculateSummaryStats(data) {
        if (!data || !Array.isArray(data)) {
            return {
                totalFiles: 0,
                distinctCount: 0,
                outdatedCount: 0,
                paidCount: 0
            };
        }

        const distinctCount = data.length;
        const outdatedCount = data.filter(v => 
            v.versionAge === 'VERY_OLD' || v.versionAge === 'OLD'
        ).length;
        const paidCount = data.filter(v => v.requiresCommercialLicense).length;

        // Estimate total files from distinct versions (each version may represent multiple files)
        const totalFiles = data.reduce((sum, v) => sum + (v.fileCount || 1), 0);

        return {
            totalFiles,
            distinctCount,
            outdatedCount,
            paidCount
        };
    }

    /**
     * Format version age enum to display text
     * @param {string} age - Version age enum value
     * @returns {Object} Display text and color indicator
     */
    function formatVersionAge(age) {
        const ageMap = {
            'VERY_OLD': { text: 'Very Old (<11)', color: 'danger' },
            'OLD': { text: 'Old (11-20)', color: 'warning' },
            'OK': { text: 'OK (21+)', color: 'success' }
        };
        return ageMap[age] || { text: 'Unknown', color: 'warning' };
    }

    /**
     * Format license status for display
     * @param {boolean} requiresLicense - Whether commercial license is required
     * @param {string} explanation - License explanation text
     * @returns {Object} License display info
     */
    function formatLicenseStatus(requiresLicense, explanation) {
        if (requiresLicense) {
            return {
                text: 'Commercial License Required',
                shortText: 'Commercial',
                explanation: explanation || 'This version requires a commercial license.',
                color: 'warning'
            };
        }
        return {
            text: 'Free / Open Source',
            shortText: 'Free',
            explanation: explanation || 'No commercial license required.',
            color: 'success'
        };
    }

    /**
     * Generate filename with current date
     * @returns {string} Filename in format: java-version-analysis-YYYY-MM-DD.pdf
     */
    function generateFilename() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `java-version-analysis-${year}-${month}-${day}.pdf`;
    }

    /**
     * Format timestamp for PDF header
     * @returns {string} Formatted timestamp
     */
    function formatTimestamp() {
        const now = new Date();
        return now.toLocaleString('en-US', {
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            timeZoneName: 'short'
        });
    }

    /**
     * Generate PDF report
     * @param {Array} data - Analysis data
     * @param {Object} stats - Summary statistics
     * @returns {jsPDF} Generated PDF document
     */
    function generatePDF(data, stats) {
        const { jsPDF } = window.jspdf;
        
        // Create PDF document (A4, portrait)
        const doc = new jsPDF({
            orientation: 'portrait',
            unit: 'mm',
            format: 'a4'
        });

        let currentY = CONFIG.margins.top;
        const pageWidth = doc.internal.pageSize.width;
        const pageHeight = doc.internal.pageSize.height;

        // Header - Title
        doc.setFontSize(CONFIG.fonts.header);
        doc.setTextColor(...CONFIG.colors.textDark);
        doc.setFont('helvetica', 'bold');
        doc.text('Java Version Analysis Report', CONFIG.margins.left, currentY);
        currentY += 8;

        // Header - Timestamp
        doc.setFontSize(CONFIG.fonts.small);
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(100, 100, 100);
        doc.text(`Generated: ${formatTimestamp()}`, CONFIG.margins.left, currentY);
        currentY += 12;

        // Summary Statistics Section
        doc.setFontSize(CONFIG.fonts.subheader);
        doc.setTextColor(...CONFIG.colors.primary);
        doc.setFont('helvetica', 'bold');
        doc.text('Summary', CONFIG.margins.left, currentY);
        currentY += 6;

        doc.setDrawColor(...CONFIG.colors.border);
        doc.line(CONFIG.margins.left, currentY, pageWidth - CONFIG.margins.right, currentY);
        currentY += 8;

        // Stats grid
        doc.setFontSize(CONFIG.fonts.body);
        doc.setTextColor(...CONFIG.colors.textDark);
        doc.setFont('helvetica', 'normal');

        const statsData = [
            ['Total Files Analyzed:', String(stats.totalFiles)],
            ['Distinct Versions:', String(stats.distinctCount)],
            ['Outdated Versions:', String(stats.outdatedCount)],
            ['Paid License Versions:', String(stats.paidCount)]
        ];

        statsData.forEach(([label, value]) => {
            doc.setFont('helvetica', 'bold');
            doc.text(label, CONFIG.margins.left, currentY);
            doc.setFont('helvetica', 'normal');
            doc.text(value, CONFIG.margins.left + 50, currentY);
            currentY += 6;
        });

        currentY += 8;

        // Version Details Table
        doc.setFontSize(CONFIG.fonts.subheader);
        doc.setTextColor(...CONFIG.colors.primary);
        doc.setFont('helvetica', 'bold');
        doc.text('Version Details', CONFIG.margins.left, currentY);
        currentY += 6;

        doc.setDrawColor(...CONFIG.colors.border);
        doc.line(CONFIG.margins.left, currentY, pageWidth - CONFIG.margins.right, currentY);
        currentY += 4;

        // Prepare table data
        const tableHead = [['Java Version', 'Runtime', 'VM', 'Vendor', 'Age', 'License']];
        const tableBody = data.map(version => {
            const age = formatVersionAge(version.versionAge);
            const license = formatLicenseStatus(
                version.requiresCommercialLicense,
                version.licenseExplanation
            );

            return [
                version.javaVersion || 'N/A',
                (version.javaRuntimeVersion || 'N/A').substring(0, 20),
                (version.javaVmVersion || 'N/A').substring(0, 20),
                (version.javaVendor || version.javaVmVendor || 'N/A').substring(0, 15),
                age.text,
                license.shortText
            ];
        });

        // Add table using autoTable
        doc.autoTable({
            head: tableHead,
            body: tableBody,
            startY: currentY,
            theme: 'grid',
            headStyles: {
                fillColor: CONFIG.colors.primary,
                textColor: CONFIG.colors.textLight,
                fontSize: CONFIG.fonts.small,
                fontStyle: 'bold',
                halign: 'center'
            },
            bodyStyles: {
                fontSize: CONFIG.fonts.small,
                textColor: CONFIG.colors.textDark
            },
            alternateRowStyles: {
                fillColor: CONFIG.colors.background
            },
            columnStyles: {
                0: { cellWidth: 25 }, // Java Version
                1: { cellWidth: 30 }, // Runtime
                2: { cellWidth: 30 }, // VM
                3: { cellWidth: 25 }, // Vendor
                4: { cellWidth: 28 }, // Age
                5: { cellWidth: 28 }  // License
            },
            styles: {
                cellPadding: 2,
                fontSize: CONFIG.fonts.small,
                valign: 'middle'
            },
            pageBreak: 'auto',
            showHead: 'everyPage',
            didDrawPage: function(data) {
                // Footer on each page
                const pageCount = doc.internal.getNumberOfPages();
                const currentPage = data.pageNumber;
                
                doc.setFontSize(CONFIG.fonts.small);
                doc.setTextColor(100, 100, 100);
                doc.setFont('helvetica', 'normal');
                
                // Footer text
                const footerText = `Java Version Checker | Page ${currentPage} of ${pageCount}`;
                const textWidth = doc.getTextWidth(footerText);
                doc.text(footerText, (pageWidth - textWidth) / 2, pageHeight - 10);
            }
        });

        return doc;
    }

    /**
     * Download PDF to user's device
     * @param {jsPDF} doc - Generated PDF document
     * @param {string} filename - Download filename
     */
    function downloadPDF(doc, filename) {
        doc.save(filename);
    }

    /**
     * Check browser compatibility for PDF generation
     * @returns {boolean} True if browser supports PDF generation
     */
    function checkBrowserCompatibility() {

        // Check for jsPDF availability
        if (typeof window.jspdf === 'undefined') {
            console.error('[PDF Export] jsPDF not found - window.jspdf is undefined');
            return false;
        }

        // Check for jsPDF constructor
        if (typeof window.jspdf.jsPDF === 'undefined') {
            console.error('[PDF Export] jsPDF constructor not found');
            return false;
        }

        // Create a test instance to check if autoTable is available
        try {
            const testDoc = new window.jspdf.jsPDF();
            if (typeof testDoc.autoTable !== 'function') {
                console.error('[PDF Export] autoTable plugin not found on jsPDF instance');
                return false;
            }
        } catch (error) {
            console.error('[PDF Export] Error creating test PDF instance:', error);
            return false;
        }

        return true;
    }

    /**
     * Main export function - triggered by download button
     * @returns {Promise<boolean>} Success status
     */
    async function exportToPDF() {

        // Check browser compatibility
        if (!checkBrowserCompatibility()) {
            console.error('[PDF Export] Browser compatibility check failed');
            alert('PDF generation is not supported in your browser. Please use a modern browser like Chrome, Firefox, Safari, or Edge.');
            return false;
        }

        // Extract and validate data
        const data = extractAnalysisData();
        if (!data) {
            console.error('[PDF Export] No analysis data found');
            alert('No analysis data available. Please run an analysis first.');
            return false;
        }

        try {
            const stats = calculateSummaryStats(data);

            const doc = generatePDF(data, stats);

            const filename = generateFilename();

            downloadPDF(doc, filename);

            return true;
        } catch (error) {
            console.error('[PDF Export] Generation failed:', error);
            console.error('[PDF Export] Stack trace:', error.stack);
            alert('Failed to generate PDF. Please try again or contact support if the issue persists.');
            return false;
        }
    }

    /**
     * Initialize PDF export functionality for a page
     * @param {string} buttonId - ID of the download button element
     */
    function init(buttonId) {

        const button = document.getElementById(buttonId);
        if (!button) {
            console.warn(`[PDF Export] Button with id "${buttonId}" not found`);
            return;
        }

        // Check if data exists
        const data = extractAnalysisData();
        const hasData = data !== null;

        // Always show the button - let the click handler handle missing data
        button.style.display = 'flex';

        // Bind click handler
        button.addEventListener('click', async function(e) {
            e.preventDefault();
            e.stopPropagation();

            // Add loading state
            const originalText = button.innerHTML;
            button.innerHTML = '<span class="spinner-small"></span>';
            button.disabled = true;
            await exportToPDF();
// Restore button state
            button.innerHTML = originalText;
            button.disabled = false;
        });
        
    }

    /**
     * Update button visibility based on data availability
     * Call this when data becomes available (e.g., after form submission)
     * @param {string} buttonId - ID of the download button
     * @param {boolean} visible - Whether to show the button
     */
    function setButtonVisibility(buttonId, visible) {
        const button = document.getElementById(buttonId);
        if (button) {
            button.style.display = visible ? 'flex' : 'none';
        }
    }

    // Expose public API
    window.PDFExport = {
        init,
        setButtonVisibility,
        exportToPDF,
        extractAnalysisData,
        checkBrowserCompatibility
    };

})();
