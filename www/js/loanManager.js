/**
 * 贷款管理器 - 前端数据管理和计算逻辑
 * 将 Flask 后端的 Python 逻辑移植到 JavaScript
 */

const LoanManager = (function() {
    // 数据存储键名
    const STORAGE_KEYS = {
        LOANS: 'loan_tracker_loans',
        PAYMENTS: 'loan_tracker_payments'
    };

    // 加载数据
    function loadData() {
        const loans = JSON.parse(localStorage.getItem(STORAGE_KEYS.LOANS) || '[]');
        const payments = JSON.parse(localStorage.getItem(STORAGE_KEYS.PAYMENTS) || '[]');
        return { loans, payments };
    }

    // 保存数据
    function saveData(data) {
        localStorage.setItem(STORAGE_KEYS.LOANS, JSON.stringify(data.loans));
        localStorage.setItem(STORAGE_KEYS.PAYMENTS, JSON.stringify(data.payments));
    }

    // 缓存计算结果，避免重复计算
    const calculationCache = new Map();
    
    // 生成缓存键
    function getCacheKey(principal, annualRate, months, method) {
        return `${principal}_${annualRate}_${months}_${method}`;
    }

    // 计算每月还款金额（移植自 Python calculate_monthly_payment）
    // 优化：添加缓存机制，减少重复计算
    function calculateMonthlyPayment(principal, annualRate, months, method = 'equal_interest') {
        const cacheKey = getCacheKey(principal, annualRate, months, method);
        if (calculationCache.has(cacheKey)) {
            return calculationCache.get(cacheKey);
        }
        
        let result;
        
        if (annualRate === 0) {
            result = principal / months;
            calculationCache.set(cacheKey, result);
            return result;
        }

        const monthlyRate = annualRate / 12 / 100;

        if (method === 'equal_interest') {
            // 等额本息
            if (monthlyRate === 0) {
                result = principal / months;
            } else {
                const monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, months)) / 
                                       (Math.pow(1 + monthlyRate, months) - 1);
                result = Math.round(monthlyPayment * 100) / 100;
            }

        } else if (method === 'equal_principal') {
            // 等额本金：首月还款 = (本金/期数) + 本金×月利率
            const basePrincipal = principal / months;
            const firstMonthInterest = principal * monthlyRate;
            result = Math.round((basePrincipal + firstMonthInterest) * 100) / 100;

        } else if (method === 'interest_first') {
            // 先息后本：每月只还利息
            result = Math.round(principal * monthlyRate * 100) / 100;

        } else if (method === 'lump_sum') {
            // 利随本清：到期一次性还本付息
            const totalInterest = principal * monthlyRate * months;
            result = Math.round((principal + totalInterest) * 100) / 100;

        } else {
            // 默认等额本息
            if (monthlyRate === 0) {
                result = principal / months;
            } else {
                const monthlyPayment = principal * (monthlyRate * Math.pow(1 + monthlyRate, months)) / 
                                       (Math.pow(1 + monthlyRate, months) - 1);
                result = Math.round(monthlyPayment * 100) / 100;
            }
        }
        
        // 缓存结果
        calculationCache.set(cacheKey, result);
        return result;
    }

    // 计算贷款剩余金额和新的月供（移植自 Python calculate_remaining_loan）
    function calculateRemainingLoan(loan, payments) {
        const loanPayments = payments.filter(p => p.loan_id === loan.id);
        const totalPaid = loanPayments.reduce((sum, p) => sum + p.amount, 0);
        
        let remainingPrincipal = loan.principal - totalPaid;
        const repaymentMethod = loan.repayment_method || 'equal_interest';

        // 信用卡特殊计算
        if (loan.loan_type === 'credit_card') {
            const isPaidOff = remainingPrincipal <= 0;

            if (isPaidOff) {
                return {
                    remaining_principal: 0,
                    remaining_months: 0,
                    new_monthly_payment: 0,
                    total_paid: totalPaid,
                    is_paid_off: true,
                    repayment_method: repaymentMethod
                };
            }

            let newMonthlyPayment, remainingMonths;

            if (repaymentMethod === 'lump_sum') {
                newMonthlyPayment = Math.round(remainingPrincipal * 100) / 100;
                remainingMonths = 1;
            } else if (repaymentMethod === 'equal_interest') {
                const months = loan.months || 12;
                newMonthlyPayment = calculateMonthlyPayment(remainingPrincipal, loan.annual_rate, months, 'equal_interest');
                remainingMonths = months;
            } else if (repaymentMethod === 'equal_principal') {
                const months = loan.months || 12;
                newMonthlyPayment = calculateMonthlyPayment(remainingPrincipal, loan.annual_rate, months, 'equal_principal');
                remainingMonths = months;
            } else if (repaymentMethod === 'interest_first') {
                newMonthlyPayment = calculateMonthlyPayment(remainingPrincipal, loan.annual_rate, 12, 'interest_first');
                remainingMonths = 12;
            } else {
                newMonthlyPayment = Math.round(remainingPrincipal * 100) / 100;
                remainingMonths = 1;
            }

            return {
                remaining_principal: Math.max(0, Math.round(remainingPrincipal * 100) / 100),
                remaining_months: remainingMonths,
                new_monthly_payment: newMonthlyPayment,
                total_paid: totalPaid,
                is_paid_off: false,
                repayment_method: repaymentMethod
            };
        }

        // 普通贷款计算
        const startDate = new Date(loan.start_date);
        const today = new Date();
        const monthsPassed = (today.getFullYear() - startDate.getFullYear()) * 12 + 
                            (today.getMonth() - startDate.getMonth());
        
        let remainingMonths = Math.max(0, loan.months - monthsPassed);

        if (remainingMonths === 0 || remainingPrincipal <= 0) {
            return {
                remaining_principal: Math.max(0, remainingPrincipal),
                remaining_months: 0,
                new_monthly_payment: 0,
                total_paid: totalPaid,
                is_paid_off: remainingPrincipal <= 0,
                repayment_method: repaymentMethod
            };
        }

        const newMonthlyPayment = calculateMonthlyPayment(
            remainingPrincipal,
            loan.annual_rate,
            remainingMonths,
            repaymentMethod
        );

        return {
            remaining_principal: Math.round(remainingPrincipal * 100) / 100,
            remaining_months: remainingMonths,
            new_monthly_payment: newMonthlyPayment,
            total_paid: totalPaid,
            is_paid_off: false,
            repayment_method: repaymentMethod
        };
    }

    // 获取所有贷款及其状态
    function getLoans() {
        const data = loadData();
        return data.loans.map(loan => {
            const status = calculateRemainingLoan(loan, data.payments);
            return { ...loan, status };
        });
    }

    // 创建新贷款
    function createLoan(loanData) {
        const data = loadData();
        const loanType = loanData.loan_type || 'normal';
        const repaymentMethod = loanData.repayment_method || 'equal_interest';

        const newLoan = {
            id: data.loans.length > 0 ? Math.max(...data.loans.map(l => l.id)) + 1 : 1,
            name: loanData.name,
            loan_type: loanType,
            repayment_method: repaymentMethod,
            principal: parseFloat(loanData.principal),
            annual_rate: parseFloat(loanData.annual_rate),
            months: parseInt(loanData.months),
            start_date: loanData.start_date || new Date().toISOString().split('T')[0]
        };

        // 信用卡特殊字段
        if (loanType === 'credit_card') {
            newLoan.credit_limit = loanData.credit_limit || 0;
            newLoan.due_date = loanData.due_date || 1;
            if (repaymentMethod === 'equal_interest' || repaymentMethod === 'equal_principal') {
                newLoan.months = parseInt(loanData.months) || 12;
            } else {
                newLoan.months = parseInt(loanData.months) || 1;
            }
            newLoan.original_monthly_payment = calculateMonthlyPayment(
                parseFloat(loanData.principal),
                parseFloat(loanData.annual_rate || 18),
                newLoan.months,
                repaymentMethod
            );
        } else {
            newLoan.original_monthly_payment = calculateMonthlyPayment(
                parseFloat(loanData.principal),
                parseFloat(loanData.annual_rate),
                parseInt(loanData.months)
            );
        }

        data.loans.push(newLoan);
        saveData(data);
        return newLoan;
    }

    // 删除贷款
    function deleteLoan(loanId) {
        const data = loadData();
        data.loans = data.loans.filter(l => l.id !== loanId);
        data.payments = data.payments.filter(p => p.loan_id !== loanId);
        saveData(data);
        return { message: '贷款已删除' };
    }

    // 还款
    function makePayment(paymentData) {
        const data = loadData();
        const loanId = paymentData.loan_id;
        const amount = parseFloat(paymentData.amount);

        const loan = data.loans.find(l => l.id === loanId);
        if (!loan) {
            throw new Error('贷款不存在');
        }

        const status = calculateRemainingLoan(loan, data.payments);
        if (amount > status.remaining_principal) {
            throw new Error(`还款金额不能超过剩余本金 ${status.remaining_principal} 元`);
        }

        const newPayment = {
            id: data.payments.length > 0 ? Math.max(...data.payments.map(p => p.id)) + 1 : 1,
            loan_id: loanId,
            amount: amount,
            date: paymentData.date || new Date().toISOString().split('T')[0],
            note: paymentData.note || ''
        };

        data.payments.push(newPayment);
        saveData(data);

        const updatedStatus = calculateRemainingLoan(loan, data.payments);
        return { payment: newPayment, loan_status: updatedStatus };
    }

    // 获取指定贷款的还款记录
    function getLoanPayments(loanId) {
        const data = loadData();
        return data.payments.filter(p => p.loan_id === loanId);
    }

    // 获取还款计划表
    function getPaymentSchedule(loanId) {
        const data = loadData();
        const loan = data.loans.find(l => l.id === loanId);
        
        if (!loan) {
            throw new Error('贷款不存在');
        }

        const payments = data.payments.filter(p => p.loan_id === loanId);
        const totalPaid = payments.reduce((sum, p) => sum + p.amount, 0);
        
        let remainingPrincipal = loan.principal - totalPaid;
        const monthlyRate = loan.annual_rate / 12 / 100;
        const repaymentMethod = loan.repayment_method || 'equal_interest';
        const isCreditCard = loan.loan_type === 'credit_card';

        const startDate = new Date(loan.start_date);
        const today = new Date();
        const monthsPassed = (today.getFullYear() - startDate.getFullYear()) * 12 + 
                            (today.getMonth() - startDate.getMonth());

        let remainingMonths;
        if (isCreditCard) {
            remainingMonths = loan.months;
        } else {
            remainingMonths = Math.max(0, loan.months - monthsPassed);
        }

        const schedule = [];
        let currentBalance = remainingPrincipal;
        const originalPrincipal = loan.principal;

        if (remainingMonths > 0 && currentBalance > 0) {
            const basePrincipal = originalPrincipal / loan.months;

            for (let month = 1; month <= remainingMonths; month++) {
                if (currentBalance <= 0) break;

                let monthlyPayment, principalPayment, interest;

                if (repaymentMethod === 'equal_interest') {
                    monthlyPayment = calculateMonthlyPayment(
                        currentBalance,
                        loan.annual_rate,
                        remainingMonths - month + 1,
                        'equal_interest'
                    );
                    interest = currentBalance * monthlyRate;
                    principalPayment = monthlyPayment - interest;

                } else if (repaymentMethod === 'equal_principal') {
                    interest = currentBalance * monthlyRate;
                    principalPayment = basePrincipal;
                    monthlyPayment = principalPayment + interest;

                } else if (repaymentMethod === 'interest_first') {
                    interest = currentBalance * monthlyRate;
                    if (month === remainingMonths) {
                        principalPayment = currentBalance;
                        monthlyPayment = principalPayment + interest;
                    } else {
                        principalPayment = 0;
                        monthlyPayment = interest;
                    }

                } else if (repaymentMethod === 'lump_sum') {
                    if (isCreditCard) {
                        interest = currentBalance * monthlyRate;
                        if (month === remainingMonths) {
                            principalPayment = currentBalance;
                            monthlyPayment = principalPayment + interest;
                        } else {
                            principalPayment = 0;
                            monthlyPayment = interest;
                        }
                    } else {
                        if (month === remainingMonths) {
                            interest = currentBalance * monthlyRate * remainingMonths;
                            principalPayment = currentBalance;
                            monthlyPayment = principalPayment + interest;
                        } else {
                            interest = 0;
                            principalPayment = 0;
                            monthlyPayment = 0;
                        }
                    }
                } else {
                    monthlyPayment = calculateMonthlyPayment(
                        currentBalance,
                        loan.annual_rate,
                        remainingMonths - month + 1,
                        'equal_interest'
                    );
                    interest = currentBalance * monthlyRate;
                    principalPayment = monthlyPayment - interest;
                }

                if (principalPayment > currentBalance) {
                    principalPayment = currentBalance;
                    monthlyPayment = principalPayment + interest;
                }

                currentBalance -= principalPayment;

                // 计算还款日期
                let paymentDate;
                if (isCreditCard) {
                    const dueDate = loan.due_date || 1;
                    let paymentYear = startDate.getFullYear();
                    let paymentMonth = startDate.getMonth() + month + 1;
                    while (paymentMonth > 12) {
                        paymentMonth -= 12;
                        paymentYear++;
                    }
                    const lastDayOfMonth = new Date(paymentYear, paymentMonth, 0).getDate();
                    const actualDueDate = Math.min(dueDate, lastDayOfMonth);
                    paymentDate = new Date(paymentYear, paymentMonth - 1, actualDueDate);
                } else {
                    let paymentYear = startDate.getFullYear();
                    let paymentMonth = startDate.getMonth() + month + 1;
                    while (paymentMonth > 12) {
                        paymentMonth -= 12;
                        paymentYear++;
                    }
                    const lastDayOfMonth = new Date(paymentYear, paymentMonth, 0).getDate();
                    const actualDay = Math.min(startDate.getDate(), lastDayOfMonth);
                    paymentDate = new Date(paymentYear, paymentMonth - 1, actualDay);
                }

                schedule.push({
                    month: month,
                    date: paymentDate.toISOString().split('T')[0],
                    payment: Math.round(monthlyPayment * 100) / 100,
                    principal: Math.round(principalPayment * 100) / 100,
                    interest: Math.round(interest * 100) / 100,
                    remaining_balance: Math.round(currentBalance * 100) / 100
                });
            }
        }

        return {
            loan: loan,
            total_paid_so_far: totalPaid,
            remaining_principal: Math.round(remainingPrincipal * 100) / 100,
            schedule: schedule
        };
    }

    // 导出数据（用于备份）
    function exportData() {
        const data = loadData();
        return JSON.stringify(data, null, 2);
    }

    // 导入数据（用于恢复）
    function importData(jsonString) {
        try {
            const data = JSON.parse(jsonString);
            if (data.loans && data.payments) {
                saveData(data);
                return { success: true, message: '数据导入成功' };
            } else {
                throw new Error('数据格式不正确');
            }
        } catch (e) {
            throw new Error('导入失败: ' + e.message);
        }
    }

    // 清空所有数据
    function clearAllData() {
        localStorage.removeItem(STORAGE_KEYS.LOANS);
        localStorage.removeItem(STORAGE_KEYS.PAYMENTS);
    }

    // 公共 API
    return {
        getLoans,
        createLoan,
        deleteLoan,
        makePayment,
        getLoanPayments,
        getPaymentSchedule,
        exportData,
        importData,
        clearAllData,
        calculateMonthlyPayment,
        calculateRemainingLoan
    };
})();

// 兼容旧版浏览器
if (typeof module !== 'undefined' && module.exports) {
    module.exports = LoanManager;
}
