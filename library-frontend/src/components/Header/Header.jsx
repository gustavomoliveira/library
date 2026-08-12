import styles from './Header.module.css';

export default function Header() {
    return (
        <header className={styles.header}>
            <div>
                <p className={styles.subtitle}>Sistema de Gestão</p>
                <h1 className={styles.title}>Biblio<span>teca</span></h1>
            </div>
        </header>
    )
}