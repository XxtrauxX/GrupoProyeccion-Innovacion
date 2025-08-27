package com.grupoproyeccion.play;

import com.grupoproyeccion.play.model.AccountBancolombia;
import com.grupoproyeccion.play.modules.bancolombia_account.BancolombiaAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
class PlayApplicationTests {

	@Test
	void contextLoads() {
	}

	// --- MÉTODO DE PRUEBA AÑADIDO ---
	@Test
	void testBancolombiaAccountService() {
		// 1. Arrange: Preparamos los datos de prueba
		BancolombiaAccountService bancolombiaService = new BancolombiaAccountService();
		String sampleText = "HASTA: 2025/06/30\n" +
							"FECHA DESCRIPCIÓN SUCURSAL DCTO. VALOR SALDO\n" +
							"27/04 TRANSFERENCIA CTA SUC VIRTUAL 12,500.00 12,500.00\n" +
							"27/04 TRANSFERENCIA A NEQUI -10,000.00 2,500.00\n" +
							"28/04 ABONO INTERESES AHORROS .52 390,881.05\n" +
							"FIN ESTADO DE CUENTA";

		// 2. Act: Ejecutamos el método que queremos probar
		List<AccountBancolombia> transactions = bancolombiaService.processText(sampleText);

		// 3. Assert: Verificamos que los resultados sean los esperados
		assertFalse(transactions.isEmpty(), "La lista de transacciones no debería estar vacía.");
		assertEquals(3, transactions.size(), "Deberían haberse encontrado 3 transacciones.");

		// Verificamos los datos de la segunda transacción para más detalle
		AccountBancolombia secondTransaction = transactions.get(1);
		assertEquals("27/04/2025", secondTransaction.getDate(), "La fecha no es correcta.");
		assertEquals("TRANSFERENCIA A NEQUI", secondTransaction.getDescription(), "La descripción no es correcta.");
		assertEquals(-10000.00, secondTransaction.getValue(), "El valor no es correcto.");
	}
}