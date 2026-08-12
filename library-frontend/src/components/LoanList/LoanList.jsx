import styles from "./LoanList.module.css";
import { getAllLoans, getBookById, getUserById } from "../../services/index.js";
import { useEffect, useState } from "react";

export default function LoanList() {
    const [loans, setLoans] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchLoans = async () => {
            try {
                const loansData = await getAllLoans();

                const detailedLoans = await Promise.all(
                    loansData.map(async (loan) => {
                        const book = await getBookById(loan.bookId);
                        const user = await getUserById(loan.userId);
                        return {...loan, bookTitle: book.title, userName: user.name};
                    })
                );
                setLoans(detailedLoans);
            } catch (e) {
                setError(e);
            } finally {
                setLoading(false);
            }
        };
        fetchLoans();
    }, [])

    return (
        <>
            {error ? (
                <p className={styles.errorMessage}>Erro ao carregar dados de empréstimo</p>
            ) : loading ? (
                <p className={styles.loadingMessage}>Carregando...</p>
            ) : (
                <section className={styles.section}>
                    <div className={styles.sectionHeader}>
                        <h2 className={styles.sectionTitle}>Empréstimos</h2>
                        <span className={styles.count}>{loans.length} registros</span>
                    </div>
                    {loans.length === 0 ? (
                        <p className={styles.emptyMessage}>Nenhum empréstimo encontrado.</p>
                    ) : (
                        <ul className={styles.list}>
                            {loans.map((loan) => (
                                <li key={loan.id} className={styles.card}>
                                    <span className={styles.loanId}>#{loan.id}</span>
                                    <div className={styles.bookInfo}>
                                        <span className={styles.label}>Livro</span>
                                        <span className={styles.value}>{loan.bookTitle}</span>
                                    </div>
                                    <div className={styles.dateInfo}>
                                        <span className={styles.label}>Usuário</span>
                                        <span className={styles.value}>{loan.userName}</span>
                                    </div>
                                    <div>
                                        {loan.returnDate
                                            ? <span className={`${styles.badge} ${styles.badgeReturned}`}>Devolvido {loan.returnDate}</span>
                                            : <span className={`${styles.badge} ${styles.badgeActive}`}>Ativo desde {loan.loanDate}</span>
                                        }
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            )}
        </>
    )
}