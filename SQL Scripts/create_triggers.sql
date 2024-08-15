USE hvs;

DROP TRIGGER IF EXISTS cashier_assignments_after_update;
DELIMITER //
CREATE TRIGGER cashier_assignments_after_update
    BEFORE UPDATE ON cashier_assignments
    FOR EACH ROW
BEGIN
    -- only limitation is that, if the register has a NULL cashier_id, then there will be a sign out
    -- for a NULL cashier
    INSERT INTO cashier_assignments_audit
    VALUES (OLD.register_id, OLD.cashier_id, 'Sign out', NOW() );

    INSERT INTO cashier_assignments_audit
    VALUES (NEW.register_id, NEW.cashier_id, 'Sign in', NOW() );

    -- fixes edge case, removing rows where a NULL employee logins
    DELETE FROM cashier_assignments_audit
    WHERE cashier_id IS NULL;
END //
DELIMITER ;
