import styles from './FineList.module.css';
import { getAllFines, getUserById, payFine } from "../../services/index.js";
import { useEffect, useState } from "react";

export default function FineList() {
    const [fines, setFines] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [payingId, setPayingId] = useState(null);

    const fetchFines = async () => {
        try {
            const finesData = await getAllFines();

            const detailedFines = await Promise.all(
                finesData.map(async (fine) => {
                    const user = await getUserById(fine.userId);
                    return {...fine, userName: user.name};
                })
            );
            setFines(detailedFines);
        } catch (e) {
            setError(e);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFines();
    }, []);

    const handlePay = async (fineId) => {
        setPayingId(fineId);
        try {
            await payFine(fineId);
            await fetchFines();
        } catch (e) {
            setError(e);
        } finally {
            setPayingId(null);
        }
    };

    const formatAmount = (amount) =>
        new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'}).format(amount);

    return (
        <>
            {error ? (
                <p className={styles.errorMessage}>Erro ao carregar dados de multas</p>
            ) : loading ? (
                <p className={styles.loadingMessage}>Carregando...</p>
            ) : (
                <section className={styles.section}>
                    <div className={styles.sectionHeader}>
                        <h2 className={styles.sectionTitle}>Multas</h2>
                        <span className={styles.count}>{fines.length} registros</span>
                    </div>
                    {fines.length === 0 ? (
                        <p className={styles.emptyMessage}>Nenhuma multa encontrada.</p>
                    ) : (
                        <ul className={styles.list}>
                            {fines.map((fine) => (
                                <li key={fine.id} className={styles.card}>
                                    <span className={styles.fineId}>#{fine.id}</span>
                                    <div className={styles.infoBlock}>
                                        <span className={styles.label}>Usuário</span>
                                        <span className={styles.value}>{fine.userName}</span>
                                    </div>
                                    <div className={styles.infoBlock}>
                                        <span className={styles.label}>Atraso</span>
                                        <span className={styles.value}>
                                            {fine.daysLate} {fine.daysLate === 1 ? 'dia' : 'dias'}
                                        </span>
                                    </div>
                                    <div className={styles.infoBlock}>
                                        <span className={styles.label}>Valor</span>
                                        <span className={styles.amount}>{formatAmount(fine.amount)}</span>
                                    </div>
                                    <div className={styles.statusArea}>
                                        {fine.status === 'PAID' ? (
                                            <span className={`${styles.badge} ${styles.badgePaid}`}>Paga</span>
                                        ) : (
                                            <>
                                                <span className={`${styles.badge} ${styles.badgePending}`}>Pendente</span>
                                                <button
                                                    className={styles.payButton}
                                                    onClick={() => handlePay(fine.id)}
                                                    disabled={payingId === fine.id}
                                                >
                                                    {payingId === fine.id ? 'Processando...' : 'Marcar como paga'}
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </li>
                            ))}
                        </ul>
                    )}
                </section>
            )}
        </>
    );
}