import styles from "./UserList.module.css";
import { getAllUsers } from "../../services/index.js";
import { useEffect, useState } from "react";

export default function UserList() {
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const response = await getAllUsers();
                setUsers(response);
            } catch (e) {
                setError(e);
            } finally {
                setLoading(false);
            }
        };
        fetchData();
    }, []);

    return (
        <>
            {error ? (
                <p className={styles.errorMessage}>Erro ao carregar dados de usuários</p>
            ) : loading ? (
                <p className={styles.loadingMessage}>Carregando...</p>
            ) : (
                <section className={styles.section}>
                    <div className={styles.sectionHeader}>
                        <h2 className={styles.sectionTitle}>Usuários</h2>
                        <span className={styles.count}>{users.length} cadastrados</span>
                    </div>
                    {users.length === 0 ? (
                        <p className={styles.emptyMessage}>Nenhum usuário encontrado.</p>
                    ) : (
                        <ul className={styles.list}>
                            {users.map((user) => (
                                <li key={user.id} className={styles.card}>
                                    <div className={styles.avatar}>
                                        {user.name.charAt(0)}
                                    </div>
                                    <div>
                                        <p className={styles.userName}>{user.name}</p>
                                        <p className={styles.userEmail}>{user.email}</p>
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