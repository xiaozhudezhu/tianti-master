package com.swinginwind.portal.gemstone.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfImportedPage;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.swinginwind.portal.common.config.AppConfig;
import com.swinginwind.portal.common.dao.CustomBaseSqlDaoImpl;
import com.swinginwind.portal.common.entity.PageModel;
import com.swinginwind.portal.common.service.CommonService;
import com.swinginwind.portal.gemstone.WebHelper;
import com.swinginwind.portal.gemstone.dao.GemstoneReportDao;
import com.swinginwind.portal.gemstone.dao.GemstoneTypeDao;
import com.swinginwind.portal.gemstone.dto.GemstoneReportQueryDTO;
import com.swinginwind.portal.gemstone.entity.GemstoneReport;
import com.swinginwind.portal.gemstone.entity.GemstoneReportImage;
import com.swinginwind.portal.gemstone.entity.GemstoneType;

@Service
public class GemstoneReportService extends CommonService<GemstoneReport, String> {

	@Autowired(required = false)
	private AppConfig appConfig;

	@Autowired
	private CustomBaseSqlDaoImpl customBaseSqlDaoImpl;
	
	@Autowired
	private GemstoneTypeDao typeDao;
	
	@Autowired
	private KvTypeService kvTypeService;

	@Autowired
	public void setDao(GemstoneReportDao dao) {
		super.setCommonDao(dao);
	}

	@SuppressWarnings("unchecked")
	public PageModel<GemstoneReport> queryGemstoneReportPage(GemstoneReportQueryDTO dto, boolean filterCreateUser) {
		Map<String, Object> params = new HashMap<String, Object>();
		StringBuilder hql = new StringBuilder();
		hql.append(" select u from GemstoneReport u where 1=1 ");
		if (StringUtils.isNotBlank(dto.getNo())) {
			hql.append(" and u.no like :no ");
			params.put("no", "%" + dto.getNo() + "%");
		}
		if (StringUtils.isNotBlank(dto.getType())) {
			GemstoneType type = typeDao.getOne(dto.getType());
			if(type != null) {
				hql.append(" and u.type.code like :typeCode");
				params.put("typeCode", type.getCode() + "%");
			}
			
		}
		if (filterCreateUser && !WebHelper.hasRole("role_admin")) {
			hql.append(" and u.createUser.id = :createUser");
			params.put("createUser", WebHelper.getUser().getId());
		}
		hql.append(" order by u.createDate desc ");
		return customBaseSqlDaoImpl.queryForPageWithParams(hql.toString(), params, dto.getCurrentPage(),
				dto.getPageSize());
	}

	public void saveGemstoneReport(GemstoneReport report, HttpServletRequest request) {
		if (StringUtils.isEmpty(report.getId())) {
			report.setCreateUser(WebHelper.getUser());
			report.setUpdateUser(WebHelper.getUser());
			report = this.save(report);
		}
			
		else {
			report.setUpdateUser(WebHelper.getUser());
			GemstoneReport report1 = this.find(report.getId());
			if (report1 != null) {
				report.setAuditFlag(report1.getAuditFlag());
				report.setDeleteFlag(report1.getDeleteFlag());
				report.setType(report1.getType());
				report.setCreateUser(report1.getCreateUser());
				this.update(report);
			}
		}
		this.generateFile(report.getId(), true, request);
		this.generateFile(report.getId(), false, request);
	}

	public void deleteGemstoneReport(String reportId) {
		this.delete(reportId);
		File file = this.getReportFile(reportId, true);
		if (file != null && file.exists())
			file.delete();
		file = this.getReportFile(reportId, false);
		if (file != null && file.exists())
			file.delete();
	}

	public void generateFile(String reportId, boolean isPrint, HttpServletRequest request) {
		GemstoneReport report = this.get(reportId);
		if (report != null) {
			// 模板路径
			String templatePath = Thread.currentThread().getContextClassLoader()
					.getResource("template" + report.getType().getCode().split("-")[0] + (isPrint ? "-print" : "") + ".pdf").getPath();
			// 生成的新文件路径
			String newPDFPath = appConfig.getFileDir() + "/reports/" + reportId + (isPrint ? "-print" : "") + ".pdf";
			File newPDFDir = new File(appConfig.getFileDir() + "/reports");
			if (!newPDFDir.exists())
				newPDFDir.mkdirs();

			PdfReader reader;
			FileOutputStream out;
			ByteArrayOutputStream bos;
			PdfStamper stamper;
			try {
				out = new FileOutputStream(newPDFPath);// 输出流
				reader = new PdfReader(templatePath);// 读取pdf模板
				bos = new ByteArrayOutputStream();
				stamper = new PdfStamper(reader, bos);
				AcroFields form = stamper.getAcroFields();
				form.setField("no", report.getNo());
				SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH);
				form.setField("reportDate", dateFormat.format(report.getReportDate()));
				form.setField("object", report.getObject());
				form.setField("identification", report.getIdentification());
				form.setField("weight", report.getWeight() + " " + "cts");
				String cutName = this.kvTypeService.getKvName(report.getCut());
				form.setField("cut", cutName != null ? cutName : report.getCut());
				String shapeName = this.kvTypeService.getKvName(report.getShape());
				form.setField("shape", shapeName != null ? shapeName : report.getShape());
				form.setField("measurements", report.getDimensionsLength() + "x" + report.getDimensionsWidth() + "x"
						+ report.getDimensionsHeight() + " mm");
				String colorName = this.kvTypeService.getKvName(report.getColor());
				form.setField("color", colorName != null ? colorName : report.getColor());
				form.setField("origin", report.getOrigin());
				form.setField("comments", report.getComments());
				form.setField("barcode_no", report.getNo());
				String colorGradeName = this.kvTypeService.getKvName(report.getColorGrade());
				form.setField("colorGrade", colorGradeName != null ? colorGradeName : report.getColorGrade());
				String clarityGradeName = this.kvTypeService.getKvName(report.getClarityGrade());
				form.setField("clarityGrade", clarityGradeName != null ? clarityGradeName : report.getClarityGrade());
				form.setField("clarityFeature", report.getClarityFeature());
				String polishName = this.kvTypeService.getKvName(report.getPolish());
				form.setField("polish", polishName != null ? polishName : report.getPolish());
				String fluorescenceName = this.kvTypeService.getKvName(report.getFluorescence());
				form.setField("fluorescence", fluorescenceName != null ? fluorescenceName : report.getFluorescence());
				String symmetryName = this.kvTypeService.getKvName(report.getSymmetry());
				form.setField("symmetry", symmetryName != null ? symmetryName : report.getSymmetry());
				form.setField("proportions", report.getProportions());
				if (report.getImages() != null && report.getImages().size() > 0) {
					for (GemstoneReportImage image : report.getImages()) {
						String filePath = appConfig.getFileDir() + "/" + image.getFilePath();
						if (new File(filePath).exists()) {
							int pageNo = form.getFieldPositions("images").get(0).page;
							Rectangle signRect = form.getFieldPositions("images").get(0).position;
							float x = signRect.getLeft();
							float y = signRect.getBottom();
							// 根据路径读取图片
							Image image1 = Image.getInstance(filePath);
							// 获取图片页面
							PdfContentByte under = stamper.getOverContent(pageNo);
							// 图片大小自适应
							image1.scaleToFit(signRect.getWidth(), signRect.getHeight());
							// 添加图片
							image1.setAbsolutePosition(x, y);
							under.addImage(image1);
						} else {
							System.out.println("image not exists");
						}
					}
				}

				int pageNo = form.getFieldPositions("qrcode").get(0).page;
				Rectangle signRect = form.getFieldPositions("qrcode").get(0).position;
				float x = signRect.getLeft();
				float y = signRect.getBottom();
				String content = request.getScheme() //当前链接使用的协议
					    +"://" + request.getServerName()//服务器地址 
					    + ":" + request.getServerPort() //端口号 
					    + "/pdfviewer#"
					    + reportId;
				String filePath = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID().toString() + ".png";
				File tempFile = new File(filePath);
				this.createQRCode(content, (int) signRect.getWidth(), (int) signRect.getHeight(), BarcodeFormat.QR_CODE,
						tempFile);
				// 根据路径读取图片
				Image image1 = Image.getInstance(filePath);
				// 获取图片页面
				PdfContentByte under = stamper.getOverContent(pageNo);
				// 图片大小自适应
				image1.scaleToFit(signRect.getWidth(), signRect.getHeight());
				// 添加图片
				image1.setAbsolutePosition(x, y);
				under.addImage(image1);
				tempFile.delete();

				int pageNo1 = form.getFieldPositions("barcode").get(0).page;
				Rectangle signRect1 = form.getFieldPositions("barcode").get(0).position;
				float x1 = signRect1.getLeft();
				float y1 = signRect1.getBottom();
				String filePath1 = System.getProperty("java.io.tmpdir") + "/" + UUID.randomUUID().toString() + ".png";
				File tempFile1 = new File(filePath1);
				this.createQRCode(report.getNo(), (int) signRect1.getWidth(), (int) signRect1.getHeight(),
						BarcodeFormat.CODE_128, tempFile1);
				// 根据路径读取图片
				Image image2 = Image.getInstance(filePath1);
				// 获取图片页面
				PdfContentByte under1 = stamper.getOverContent(pageNo1);
				// 图片大小自适应
				image2.scaleToFit(signRect1.getWidth(), signRect1.getHeight());
				// 添加图片
				image2.setAbsolutePosition(x1, y1);
				under1.addImage(image2);
				tempFile1.delete();

				stamper.setFormFlattening(true);// 如果为false，生成的PDF文件可以编辑，如果为true，生成的PDF文件不可以编辑
				stamper.close();
				Document doc = new Document();
				PdfCopy copy = new PdfCopy(doc, out);
				doc.open();
				PdfImportedPage importPage = copy.getImportedPage(new PdfReader(bos.toByteArray()), 1);
				copy.addPage(importPage);
				doc.close();
				reader.close();
				out.close();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public File getReportFile(String reportId, boolean isPrint) {
		String pdfPath = appConfig.getFileDir() + "/reports/" + reportId + (isPrint ? "-print" : "") + ".pdf";
		File file = new File(pdfPath);
		return file;
	}

	public File getReportFile(String reportId, boolean isPrint, HttpServletRequest request) {
		File file = null;
		GemstoneReport report = this.get(reportId);
		if (report != null) {
			file = this.getReportFile(reportId, isPrint);
			if(!file.exists()) {
				this.generateFile(reportId, isPrint, request);
			}
		}
		return file;
	}

	private void createQRCode(String content, int width, int height, BarcodeFormat format, File file) throws Exception {
		Map<EncodeHintType, Object> hints = new HashMap<>();
		// 内容编码格式
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		// 指定纠错等级
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		// 设置二维码边的空度，非负数
		hints.put(EncodeHintType.MARGIN, 0);
		BitMatrix bitMatrix = new MultiFormatWriter().encode(content, format, width, height, hints);
		bitMatrix = this.deleteWhite(bitMatrix);
		MatrixToImageWriter.writeToPath(bitMatrix, "png", file.toPath());// 输出原图片
	}
	
	private BitMatrix deleteWhite(BitMatrix matrix) {
	    int[] rec = matrix.getEnclosingRectangle();
	    int resWidth = rec[2] + 1;
	    int resHeight = rec[3] + 1;

	    BitMatrix resMatrix = new BitMatrix(resWidth, resHeight);
	    resMatrix.clear();
	    for (int i = 0; i < resWidth; i++) {
	        for (int j = 0; j < resHeight; j++) {
	            if (matrix.get(i + rec[0], j + rec[1]))
	                resMatrix.set(i, j);
	        }
	    }
	    
	    int width = resMatrix.getWidth();
	    int height = resMatrix.getHeight();
	    BufferedImage image = new BufferedImage(width, height,
	            BufferedImage.TYPE_INT_RGB);
	    for (int x = 0; x < width; x++) {
	        for (int y = 0; y < height; y++) {
	            image.setRGB(x, y, resMatrix.get(x, y) ? 0 : 255);// 0-黑色;255-白色
	        }
	    }
	    
	    return resMatrix;
	}

}
